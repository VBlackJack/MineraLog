#!/usr/bin/env python3
"""
MineraLog CSV to ZIP Converter
Converts CSV files to MineraLog ZIP export format.

Usage:
    python csv_to_zip.py -i minerals.csv -o export.zip
    python csv_to_zip.py -i minerals.csv -o export.zip --encrypt --password secret
"""

import argparse
import csv
import hashlib
import json
import sys
import zipfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional
import uuid

try:
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
    from cryptography.hazmat.primitives import hashes
    from cryptography.hazmat.backends import default_backend
    CRYPTO_AVAILABLE = True
except ImportError:
    CRYPTO_AVAILABLE = False
    print("Warning: cryptography module not installed. Encryption disabled.", file=sys.stderr)


def parse_csv(csv_path: Path) -> List[Dict]:
    """Parse minerals CSV file."""
    minerals = []
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            mineral = {
                'id': row.get('id', str(uuid.uuid4())),
                'name': row['name'],
                'group': row.get('group') or None,
                'formula': row.get('formula') or None,
                'crystalSystem': row.get('crystalSystem') or None,
                'mohsMin': float(row['mohsMin']) if row.get('mohsMin') else None,
                'mohsMax': float(row['mohsMax']) if row.get('mohsMax') else None,
                'cleavage': row.get('cleavage') or None,
                'fracture': row.get('fracture') or None,
                'luster': row.get('luster') or None,
                'streak': row.get('streak') or None,
                'diaphaneity': row.get('diaphaneity') or None,
                'habit': row.get('habit') or None,
                'specificGravity': float(row['specificGravity']) if row.get('specificGravity') else None,
                'fluorescence': row.get('fluorescence') or None,
                'magnetic': row.get('magnetic', '').lower() in ('true', '1', 'yes'),
                'radioactive': row.get('radioactive', '').lower() in ('true', '1', 'yes'),
                'dimensionsMm': row.get('dimensionsMm') or None,
                'weightGr': float(row['weightGr']) if row.get('weightGr') else None,
                'notes': row.get('notes') or None,
                'tags': row.get('tags', '').split(',') if row.get('tags') else [],
                'status': row.get('status', 'incomplete'),
                'createdAt': row.get('createdAt', datetime.now(timezone.utc).isoformat()),
                'updatedAt': row.get('updatedAt', datetime.now(timezone.utc).isoformat()),
            }

            # Add provenance if present
            if any(row.get(f) for f in ['site', 'locality', 'country', 'lat', 'lon']):
                mineral['provenance'] = {
                    'id': str(uuid.uuid4()),
                    'mineralId': mineral['id'],
                    'site': row.get('site') or None,
                    'locality': row.get('locality') or None,
                    'country': row.get('country') or None,
                    'latitude': float(row['lat']) if row.get('lat') else None,
                    'longitude': float(row['lon']) if row.get('lon') else None,
                    'acquiredAt': row.get('acquiredAt') or None,
                    'source': row.get('source') or None,
                    'price': float(row['price']) if row.get('price') else None,
                    'estimatedValue': float(row['estimatedValue']) if row.get('estimatedValue') else None,
                }
            else:
                mineral['provenance'] = None

            # Add storage if present
            if any(row.get(f) for f in ['place', 'container', 'box', 'slot']):
                mineral['storage'] = {
                    'id': str(uuid.uuid4()),
                    'mineralId': mineral['id'],
                    'place': row.get('place') or None,
                    'container': row.get('container') or None,
                    'box': row.get('box') or None,
                    'slot': row.get('slot') or None,
                    'nfcTagId': None,
                    'qrContent': f"mineralapp://mineral/{mineral['id']}"
                }
            else:
                mineral['storage'] = None

            mineral['photos'] = []
            minerals.append(mineral)

    return minerals


def create_checksums(files: Dict[str, bytes]) -> str:
    """Create checksums.sha256 content."""
    lines = []
    for path, content in files.items():
        sha256 = hashlib.sha256(content).hexdigest()
        lines.append(f"{path};{sha256}")
    return '\n'.join(lines)


def derive_key(password: str, salt: bytes) -> bytes:
    """Derive encryption key from password using PBKDF2."""
    if not CRYPTO_AVAILABLE:
        raise RuntimeError("Encryption requires cryptography module")

    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32,
        salt=salt,
        iterations=100000,
        backend=default_backend()
    )
    return kdf.derive(password.encode('utf-8'))


def encrypt_content(content: bytes, password: str) -> tuple:
    """Encrypt content with AES-256-GCM."""
    if not CRYPTO_AVAILABLE:
        raise RuntimeError("Encryption requires cryptography module")

    import os
    salt = os.urandom(16)
    iv = os.urandom(12)
    key = derive_key(password, salt)

    aesgcm = AESGCM(key)
    ciphertext = aesgcm.encrypt(iv, content, None)

    return ciphertext, salt.hex(), iv.hex()


def create_zip(minerals: List[Dict], output_path: Path, password: Optional[str] = None,
               media_dir: Optional[Path] = None):
    """Create ZIP export file."""

    # Create minerals.json
    minerals_json = json.dumps(minerals, indent=2, ensure_ascii=False).encode('utf-8')

    # Create manifest
    manifest = {
        'app': 'MineraLog',
        'schemaVersion': '1.0.0',
        'exportedAt': datetime.now(timezone.utc).isoformat(),
        'counts': {
            'minerals': len(minerals),
            'photos': sum(len(m.get('photos', [])) for m in minerals)
        },
        'encrypted': password is not None
    }

    if password:
        if not CRYPTO_AVAILABLE:
            print("Error: Encryption requires 'cryptography' module", file=sys.stderr)
            print("Install: pip install cryptography", file=sys.stderr)
            sys.exit(1)

        encrypted_json, salt_hex, iv_hex = encrypt_content(minerals_json, password)
        minerals_json = encrypted_json

        manifest['kdf'] = 'PBKDF2-SHA256'
        manifest['kdfParams'] = {
            'iterations': 100000,
            'saltHex': salt_hex
        }
        manifest['cipher'] = 'AES-256-GCM'
        manifest['ivHex'] = iv_hex

    manifest_json = json.dumps(manifest, indent=2).encode('utf-8')

    # Build file map
    files = {
        'minerals.json': minerals_json
    }

    # Add media files if directory provided
    if media_dir and media_dir.exists():
        for media_file in media_dir.rglob('*'):
            if media_file.is_file():
                rel_path = media_file.relative_to(media_dir)
                files[f"media/{rel_path}"] = media_file.read_bytes()

    # Create checksums
    checksums = create_checksums(files)

    # Write ZIP
    with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zf:
        zf.writestr('manifest.json', manifest_json)
        zf.writestr('checksums.sha256', checksums)
        for path, content in files.items():
            zf.writestr(path, content)

    print(f"✓ Created {output_path}")
    print(f"  Minerals: {len(minerals)}")
    print(f"  Photos: {manifest['counts']['photos']}")
    print(f"  Encrypted: {manifest['encrypted']}")


def main():
    parser = argparse.ArgumentParser(description='Convert CSV to MineraLog ZIP export')
    parser.add_argument('-i', '--input', required=True, type=Path, help='Input CSV file')
    parser.add_argument('-o', '--output', required=True, type=Path, help='Output ZIP file')
    parser.add_argument('--photos', type=Path, help='Optional photos CSV file')
    parser.add_argument('--media-dir', type=Path, help='Directory containing media files')
    parser.add_argument('--encrypt', action='store_true', help='Encrypt the export')
    parser.add_argument('--password', type=str, help='Encryption password')

    args = parser.parse_args()

    if not args.input.exists():
        print(f"Error: Input file not found: {args.input}", file=sys.stderr)
        sys.exit(1)

    if args.encrypt and not args.password:
        import getpass
        args.password = getpass.getpass("Enter encryption password: ")

    print(f"Reading {args.input}...")
    minerals = parse_csv(args.input)

    print(f"Creating ZIP export...")
    create_zip(minerals, args.output, args.password, args.media_dir)

    print("Done!")


if __name__ == '__main__':
    main()
