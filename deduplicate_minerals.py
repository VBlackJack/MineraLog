#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MineraLog - Reference Minerals Deduplication Script
====================================================

Purpose:
    Clean reference_minerals_v5.json by removing duplicates and preparing
    for v6 with image support (imageUrl, localIconName).

Strategy:
    - Primary key: nameFr (normalized: lowercase + trim)
    - Conflict resolution: Merge entries keeping most complete fields
    - Description priority: Keep longest description
    - Add v6 fields: imageUrl and localIconName (initialized to null/"")

Author: MineraLog Development Team
Version: 3.3.0
Date: 2025-11-20
"""

import json
import sys
from pathlib import Path
from typing import Dict, List, Any
from collections import defaultdict


def normalize_name(name: str) -> str:
    """
    Normalize mineral name for comparison.

    Args:
        name: Original mineral name (e.g., "Magnétite", " QUARTZ ")

    Returns:
        Normalized name (lowercase, trimmed)
    """
    return name.strip().lower()


def count_filled_fields(mineral: Dict[str, Any]) -> int:
    """
    Count non-empty fields in a mineral entry.

    Args:
        mineral: Mineral dictionary

    Returns:
        Number of fields with non-null, non-empty values
    """
    filled = 0
    for key, value in mineral.items():
        if key in ['id', 'createdAt', 'updatedAt', 'isUserDefined', 'source']:
            continue  # Skip metadata fields

        if value is not None:
            if isinstance(value, str):
                if value.strip():  # Non-empty string
                    filled += 1
            elif isinstance(value, (int, float)):
                if value != 0.0:  # Non-zero number
                    filled += 1
            elif isinstance(value, bool):
                filled += 1
            else:
                filled += 1

    return filled


def get_description_length(mineral: Dict[str, Any]) -> int:
    """
    Calculate total length of all text fields (proxy for completeness).

    Args:
        mineral: Mineral dictionary

    Returns:
        Total character count of all text fields
    """
    total = 0
    for key, value in mineral.items():
        if isinstance(value, str):
            total += len(value.strip())

    return total


def merge_minerals(minerals: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Merge multiple duplicate entries into a single complete entry.

    Strategy:
        - Keep the entry with the most filled fields as base
        - Fill missing fields from other entries
        - For conflicting non-empty fields, keep from the most complete entry

    Args:
        minerals: List of duplicate mineral entries (same nameFr)

    Returns:
        Merged mineral entry with maximum completeness
    """
    if len(minerals) == 1:
        return minerals[0]

    # Sort by completeness (most filled fields + longest descriptions first)
    sorted_minerals = sorted(
        minerals,
        key=lambda m: (count_filled_fields(m), get_description_length(m)),
        reverse=True
    )

    # Start with the most complete entry
    merged = sorted_minerals[0].copy()

    # Fill in missing fields from other entries
    for mineral in sorted_minerals[1:]:
        for key, value in mineral.items():
            # Skip metadata fields
            if key in ['id', 'createdAt', 'updatedAt']:
                continue

            # If field is empty in merged but filled in current, use current
            merged_value = merged.get(key)
            if merged_value is None or (isinstance(merged_value, str) and not merged_value.strip()):
                if value is not None and (not isinstance(value, str) or value.strip()):
                    merged[key] = value

            # Special case: For numeric fields with 0.0, prefer non-zero values
            if isinstance(merged_value, (int, float)) and merged_value == 0.0:
                if isinstance(value, (int, float)) and value != 0.0:
                    merged[key] = value

    return merged


def add_image_fields(mineral: Dict[str, Any]) -> Dict[str, Any]:
    """
    Add v6 image support fields to mineral entry.

    Args:
        mineral: Original mineral dictionary

    Returns:
        Mineral dictionary with imageUrl and localIconName fields
    """
    mineral['imageUrl'] = None
    mineral['localIconName'] = None
    return mineral


def deduplicate_minerals(input_path: Path, output_path: Path) -> None:
    """
    Main deduplication function.

    Args:
        input_path: Path to reference_minerals_v5.json
        output_path: Path to output reference_minerals_v6.json
    """
    print("=" * 80)
    print("MineraLog - Reference Minerals Deduplication v3.3.0")
    print("=" * 80)
    print()

    # Load input JSON
    print(f"📖 Loading: {input_path}")
    try:
        with open(input_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"❌ ERROR: File not found: {input_path}")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"❌ ERROR: Invalid JSON: {e}")
        sys.exit(1)

    minerals = data.get('minerals', [])
    original_count = len(minerals)
    print(f"   ✓ Loaded {original_count} minerals")
    print()

    # Group by normalized name
    print("🔍 Detecting duplicates...")
    grouped: Dict[str, List[Dict[str, Any]]] = defaultdict(list)
    for mineral in minerals:
        name_fr = mineral.get('nameFr', '')
        if not name_fr:
            print(f"   ⚠️  WARNING: Mineral without nameFr: {mineral.get('id', 'unknown')}")
            continue

        normalized = normalize_name(name_fr)
        grouped[normalized].append(mineral)

    # Identify duplicates
    duplicates = {name: entries for name, entries in grouped.items() if len(entries) > 1}
    duplicate_count = len(duplicates)
    total_duplicate_entries = sum(len(entries) for entries in duplicates.values())

    print(f"   ✓ Found {duplicate_count} duplicate names")
    print(f"   ✓ Total duplicate entries: {total_duplicate_entries}")
    print()

    if duplicate_count > 0:
        print("📋 Duplicate minerals:")
        for name, entries in sorted(duplicates.items()):
            original_name = entries[0]['nameFr']
            print(f"   • {original_name}: {len(entries)}x entries")
        print()

    # Merge duplicates
    print("🔧 Merging duplicates...")
    deduplicated = []
    merge_count = 0

    for normalized_name, entries in grouped.items():
        if len(entries) > 1:
            merged = merge_minerals(entries)
            deduplicated.append(merged)
            merge_count += 1
        else:
            deduplicated.append(entries[0])

    print(f"   ✓ Merged {merge_count} duplicate groups")
    final_count = len(deduplicated)
    print(f"   ✓ Result: {final_count} unique minerals")
    print()

    # Add v6 image fields
    print("🖼️  Adding v6 image support fields...")
    for mineral in deduplicated:
        add_image_fields(mineral)
    print("   ✓ Added imageUrl and localIconName to all entries")
    print()

    # Sort alphabetically by nameFr for readability
    print("🔤 Sorting alphabetically...")
    deduplicated.sort(key=lambda m: normalize_name(m.get('nameFr', '')))
    print("   ✓ Sorted by nameFr")
    print()

    # Prepare output data
    output_data = {
        "version": "6.0",
        "source": "MineraLog v3.3 - Deduplicated collection library (v6) with image support",
        "total_minerals": final_count,
        "created_date": data.get('created_date', '2025-11-20'),
        "changelog": [
            "Removed duplicates (61 duplicate groups merged)",
            "Added imageUrl field for cloud/web-hosted images",
            "Added localIconName field for bundled drawable icons",
            "Sorted alphabetically by nameFr",
            "Merged entries with conflict resolution (most complete data kept)"
        ],
        "minerals": deduplicated
    }

    # Write output JSON
    print(f"💾 Writing: {output_path}")
    try:
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(output_data, f, ensure_ascii=False, indent=2)
        print("   ✓ File written successfully")
    except Exception as e:
        print(f"   ❌ ERROR: Failed to write file: {e}")
        sys.exit(1)

    # Final statistics
    print()
    print("=" * 80)
    print("📊 FINAL STATISTICS")
    print("=" * 80)
    print(f"Input (v5):        {original_count} minerals")
    print(f"Duplicates found:  {duplicate_count} names ({total_duplicate_entries} entries)")
    print(f"Merged groups:     {merge_count}")
    print(f"Output (v6):       {final_count} minerals")
    print(f"Reduction:         -{original_count - final_count} entries ({((original_count - final_count) / original_count * 100):.1f}%)")
    print()
    print("✅ Deduplication completed successfully!")
    print("=" * 80)


def main():
    """Main entry point."""
    # Paths
    project_root = Path(__file__).parent
    assets_dir = project_root / "app" / "src" / "main" / "assets"

    input_file = assets_dir / "reference_minerals_v5.json"
    output_file = assets_dir / "reference_minerals_v6.json"

    # Check input file exists
    if not input_file.exists():
        print(f"❌ ERROR: Input file not found: {input_file}")
        print(f"   Please ensure reference_minerals_v5.json exists in {assets_dir}")
        sys.exit(1)

    # Run deduplication
    deduplicate_minerals(input_file, output_file)


if __name__ == "__main__":
    main()
