import json
import uuid
from datetime import datetime

# --- CONFIGURATION ---
INPUT_FILE = 'app/src/main/assets/reference_minerals_v5.json'
OUTPUT_FILE = 'app/src/main/assets/reference_minerals_v6.json'

# --- DONNÉES À INJECTER (Format corrigé pour ReferenceMineralEntity) ---
# Les 5 minéraux "stubs" enrichis + les 10 manquants identifiés par l'audit
NEW_DATA = [
    # --- CORRECTIONS / ENRICHISSEMENTS ---
    {
        "nameFr": "Anorthite", "nameEn": "Anorthite",
        "mineralGroup": "Silicates - Tectosilicates", "formula": "CaAl2Si2O8",
        "mohsMin": 6.0, "mohsMax": 6.5, "density": 2.75,
        "crystalSystem": "Triclinique", "cleavage": "Parfait {001}, bon {010}",
        "luster": "Vitreux à nacré", "streak": "Blanc",
        "careInstructions": "Éviter les chocs thermiques et les acides forts. Nettoyer à l'eau tiède savonneuse.",
        "identificationTips": "Difficile à distinguer des autres plagioclases visuellement. Angle de clivage ~94°. Associé aux roches mafiques.",
        "geologicalEnvironment": "Roches ignées mafiques (gabbros, basaltes), anorthosites, météorites.",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Biotite", "nameEn": "Biotite",
        "mineralGroup": "Silicates - Phyllosilicates", "formula": "K(Mg,Fe)3(AlSi3O10)(OH,F)2",
        "mohsMin": 2.5, "mohsMax": 3.0, "density": 3.0,
        "crystalSystem": "Monoclinique", "cleavage": "Parfait basal {001} (feuillets)",
        "luster": "Vitreux, submétallique, nacré", "streak": "Gris à blanc",
        "color": "Noir, brun foncé, brun-vert",
        "careInstructions": "Très fragile (clivage). Les feuillets se séparent. Éviter l'eau qui peut s'infiltrer.",
        "identificationTips": "Couleur sombre, clivage micacé parfait en feuillets élastiques. Se distingue de la phlogopite (plus claire).",
        "geologicalEnvironment": "Ubiquiste : Granites, pegmatites, schistes, gneiss.",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Calcite", "nameEn": "Calcite",
        "mineralGroup": "Carbonates", "formula": "CaCO3",
        "mohsMin": 3.0, "mohsMax": 3.0, "density": 2.71,
        "crystalSystem": "Trigonal", "cleavage": "Parfait rhomboédrique {10-14}",
        "luster": "Vitreux à nacré", "streak": "Blanc",
        "fluorescence": "Fréquente (rouge, orange, jaune sous UV)",
        "careInstructions": "Très sensible aux acides (vinaigre, citron). Rayable facilement (dureté 3).",
        "identificationTips": "Effervescence vive à l'acide chlorhydrique froid. Clivage rhomboédrique. Biréfringence forte.",
        "geologicalEnvironment": "Sédimentaire (calcaires), métamorphique (marbres), hydrothermal.",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Augite", "nameEn": "Augite",
        "mineralGroup": "Silicates - Inosilicates", "formula": "(Ca,Na)(Mg,Fe,Al)(Si,Al)2O6",
        "mohsMin": 5.5, "mohsMax": 6.0, "density": 3.4,
        "crystalSystem": "Monoclinique", "cleavage": "Bon à 87° et 93°",
        "luster": "Vitreux à terne", "streak": "Gris-vert",
        "color": "Noir, vert-noir",
        "careInstructions": "Durable. Sensible aux chocs thermiques violents.",
        "identificationTips": "Section des cristaux carrée (vs amphiboles losanges). Clivage à angle droit.",
        "geologicalEnvironment": "Roches magmatiques basiques (basalte, gabbro).",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Épidote", "nameEn": "Epidote",
        "mineralGroup": "Silicates - Sorosilicates", "formula": "Ca2(Al,Fe)3(SiO4)3(OH)",
        "mohsMin": 6.0, "mohsMax": 7.0, "density": 3.4,
        "crystalSystem": "Monoclinique", "cleavage": "Parfait {001}",
        "luster": "Vitreux", "streak": "Gris",
        "color": "Vert pistache caractéristique",
        "careInstructions": "Durable. Nettoyage ultrasonique déconseillé (fissures possibles).",
        "identificationTips": "Couleur vert pistache unique. Cristaux prismatiques striés.",
        "geologicalEnvironment": "Métamorphisme régional (schistes verts), skarns, altération hydrothermale.",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },

    # --- LES 10 MANQUANTS (AJOUTS) ---
    {
        "nameFr": "Rhodochrosite", "nameEn": "Rhodochrosite",
        "mineralGroup": "Carbonates", "formula": "MnCO3",
        "mohsMin": 3.5, "mohsMax": 4.0, "density": 3.7,
        "crystalSystem": "Trigonal", "color": "Rose, rouge framboise",
        "luster": "Vitreux à nacré", "cleavage": "Parfait rhomboédrique",
        "careInstructions": "Tendre et fragile. Éviter acides. Craint la lumière vive prolongée (peut ternir).",
        "identificationTips": "Couleur rose intense, structure souvent rubanée (stalactites). Effervescence faible à l'acide chaud.",
        "geologicalEnvironment": "Veines hydrothermales de basse température.",
        "typicalLocations": "Argentine (Capillitas), USA (Sweet Home Mine), Afrique du Sud.",
        "rarity": "Peu courant", "collectingDifficulty": "Moyenne",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Larimar", "nameEn": "Larimar",
        "synonyms": "Pectolite bleue",
        "mineralGroup": "Silicates - Inosilicates", "formula": "NaCa2Si3O8(OH)",
        "mohsMin": 4.5, "mohsMax": 5.0, "density": 2.8,
        "crystalSystem": "Triclinique", "color": "Bleu ciel, bleu volcanique, blanc",
        "luster": "Soyeux à vitreux",
        "careInstructions": "Craint les produits chimiques et la lumière intense prolongée (peut pâlir).",
        "identificationTips": "Motifs en 'écume de mer' bleus et blancs uniques. Structure fibreuse compacte.",
        "geologicalEnvironment": "Cavités dans basaltes (République Dominicaine uniquement).",
        "typicalLocations": "République Dominicaine (Barahona).",
        "rarity": "Rare", "collectingDifficulty": "Difficile (source unique)",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Charoïte", "nameEn": "Charoite",
        "mineralGroup": "Silicates - Inosilicates", "formula": "K(Ca,Na)2Si4O10(OH,F)·H2O",
        "mohsMin": 5.0, "mohsMax": 6.0, "density": 2.6,
        "crystalSystem": "Monoclinique", "color": "Violet, lilas, pourpre tourbillonnant",
        "luster": "Soyeux à nacré",
        "careInstructions": "Robuste. Nettoyage à l'eau savonneuse.",
        "identificationTips": "Couleur violette intense avec motifs tourbillonnants fibreux nacrés. Souvent associée à la tinaksite (orange) et l'aegirine (noire).",
        "geologicalEnvironment": "Syénites métasomatisées (Russie uniquement).",
        "typicalLocations": "Russie (Murun Massif, Sibérie).",
        "rarity": "Rare",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Sugilite", "nameEn": "Sugilite",
        "synonyms": "Luvulite",
        "mineralGroup": "Silicates - Cyclosilicates", "formula": "KNa2(Fe,Mn,Al)2Li3Si12O30",
        "mohsMin": 6.0, "mohsMax": 6.5, "density": 2.74,
        "crystalSystem": "Hexagonal", "color": "Pourpre intense, magenta, violet",
        "luster": "Vitreux à résineux",
        "careInstructions": "Durable. Pas de clivage.",
        "identificationTips": "Couleur pourpre gélatineuse ou opaque très vive. Souvent massive.",
        "geologicalEnvironment": "Syénites alcalines et gisements de manganèse stratiformes.",
        "typicalLocations": "Afrique du Sud (Wessels Mine), Japon.",
        "rarity": "Rare",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Bénitoïte", "nameEn": "Benitoite",
        "mineralGroup": "Silicates - Cyclosilicates", "formula": "BaTiSi3O9",
        "mohsMin": 6.0, "mohsMax": 6.5, "density": 3.6,
        "crystalSystem": "Hexagonal", "color": "Bleu saphir, incolore",
        "luster": "Vitreux",
        "fluorescence": "Bleu intense sous UV courts (SW) - Diagnostique !",
        "careInstructions": "Dure mais fragile aux chocs.",
        "identificationTips": "Cristaux triangulaires bipyramidaux aplatis. Fluorescence bleue spectaculaire aux UV courts.",
        "geologicalEnvironment": "Schistes bleus hydrothermaux (Californie).",
        "typicalLocations": "USA (San Benito County, Californie - quasi unique).",
        "rarity": "Très rare",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Tanzanite", "nameEn": "Tanzanite",
        "synonyms": "Zoïsite bleue",
        "mineralGroup": "Silicates - Sorosilicates", "formula": "Ca2Al3(SiO4)3(OH)",
        "mohsMin": 6.5, "mohsMax": 7.0, "density": 3.35,
        "crystalSystem": "Orthorhombique", "color": "Bleu-violet intense",
        "luster": "Vitreux",
        "careInstructions": "Sensible aux chocs thermiques (ne pas chauffer) et aux ultrasons.",
        "identificationTips": "Trichroïsme fort (bleu / violet / rouge-brun) visible au dichroscope.",
        "geologicalEnvironment": "Métamorphisme hydrothermal (Tanzanie).",
        "typicalLocations": "Tanzanie (Merelani Hills).",
        "rarity": "Rare",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Tourmaline Paraïba", "nameEn": "Paraiba Tourmaline",
        "mineralGroup": "Silicates - Cyclosilicates", "formula": "Na(Li,Al)3Al6(BO3)3Si6O18(OH)4 (+Cu, Mn)",
        "mohsMin": 7.0, "mohsMax": 7.5, "density": 3.06,
        "crystalSystem": "Trigonal", "color": "Bleu néon, Vert-bleu électrique",
        "luster": "Vitreux",
        "careInstructions": "Durable, mais inclusions fréquentes.",
        "identificationTips": "Couleur 'bleu piscine' électrique due au Cuivre. L'analyse chimique est souvent nécessaire pour confirmer le Cu.",
        "geologicalEnvironment": "Pegmatites granitiques riches en lithium.",
        "typicalLocations": "Brésil (Paraíba), Nigéria, Mozambique.",
        "rarity": "Extrêmement rare",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Béryl Rouge", "nameEn": "Red Beryl",
        "synonyms": "Bixbite (désuet)",
        "mineralGroup": "Silicates - Cyclosilicates", "formula": "Be3Al2Si6O18 (+Mn)",
        "mohsMin": 7.5, "mohsMax": 8.0, "density": 2.8,
        "crystalSystem": "Hexagonal", "color": "Rouge groseille",
        "luster": "Vitreux",
        "careInstructions": "Dur mais souvent très inclus et fracturé. Pas d'ultrasons.",
        "identificationTips": "Forme hexagonale du béryl mais couleur rouge intense. Plus rare que le diamant.",
        "geologicalEnvironment": "Rhyolites topazifères (Utah).",
        "typicalLocations": "USA (Wah Wah Mts, Utah).",
        "rarity": "Extrêmement rare",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Alexandrite", "nameEn": "Alexandrite",
        "mineralGroup": "Oxydes", "formula": "BeAl2O4 (+Cr)",
        "mohsMin": 8.5, "mohsMax": 8.5, "density": 3.73,
        "crystalSystem": "Orthorhombique", "color": "Vert (jour) / Rouge (incandescent)",
        "luster": "Vitreux",
        "careInstructions": "Très dur et durable. Excellent pour bijouterie.",
        "identificationTips": "Le changement de couleur (effet alexandrite) est le critère clé. Vert émeraude à la lumière du jour, rouge rubis à la bougie.",
        "geologicalEnvironment": "Pegmatites, micaschistes, alluvions.",
        "typicalLocations": "Russie (Oural), Brésil, Sri Lanka, Tanzanie.",
        "rarity": "Très rare",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    },
    {
        "nameFr": "Scolécite", "nameEn": "Scolecite",
        "mineralGroup": "Silicates - Zéolites", "formula": "CaAl2Si3O10·3H2O",
        "mohsMin": 5.0, "mohsMax": 5.5, "density": 2.27,
        "crystalSystem": "Monoclinique", "color": "Blanc, incolore",
        "luster": "Vitreux à soyeux",
        "careInstructions": "Fragile. Les cristaux aciculaires se brisent facilement.",
        "identificationTips": "Longues aiguilles blanches radiées, formant souvent des 'sprays'. Pyroélectrique.",
        "geologicalEnvironment": "Cavités des basaltes (trapps).",
        "typicalLocations": "Inde (Pune, Nashik), Islande.",
        "rarity": "Commun (mais esthétique)",
        "isUserDefined": False, "source": "MineraLog Standard Library"
    }
]

# --- FONCTIONS DE TRAITEMENT ---

def clean_and_enrich():
    try:
        with open(INPUT_FILE, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        minerals = data.get('minerals', [])
        print(f"Chargement de {len(minerals)} minéraux existants.")

        # 1. Dictionnaire pour dédoublonnage (clé = nom français)
        # On garde le minéral qui a le plus de champs remplis
        mineral_map = {}
        
        for m in minerals:
            # Correction taxonomie Wolframite/Scheelite/Wulfenite
            if m['nameFr'] in ['Wolframite', 'Schéelite', 'Scheelite', 'Wulfénite', 'Wulfenite']:
                if "Silicates" in (m.get('mineralGroup') or ""):
                    # On corrige le groupe
                    if "Wolframite" in m['nameFr'] or "Scheelite" in m['nameFr']:
                        m['mineralGroup'] = "Tungstates"
                    elif "Wulfenite" in m['nameFr']:
                        m['mineralGroup'] = "Molybdates"
                    print(f"Correction groupe pour {m['nameFr']}")

            key = m['nameFr'].lower().strip()
            
            # Logique de fusion : si on a déjà ce minéral, on garde celui qui a une description/careInstructions
            if key in mineral_map:
                existing = mineral_map[key]
                score_existing = len(str(existing.get('careInstructions', ''))) + len(str(existing.get('identificationTips', '')))
                score_new = len(str(m.get('careInstructions', ''))) + len(str(m.get('identificationTips', '')))
                
                if score_new > score_existing:
                    mineral_map[key] = m # On remplace par le meilleur
                    print(f"Amélioration doublon pour {m['nameFr']}")
            else:
                mineral_map[key] = m

        # 2. Injection des nouvelles données (Mise à jour ou Création)
        for new_min in NEW_DATA:
            key = new_min['nameFr'].lower().strip()
            
            if key in mineral_map:
                # Mise à jour d'un existant (ex: les 5 stubs)
                print(f"Enrichissement de {new_min['nameFr']}...")
                target = mineral_map[key]
                # On met à jour les champs, en conservant l'ID existant
                for field, value in new_min.items():
                    target[field] = value
                target['updatedAt'] = datetime.utcnow().isoformat() + "Z"
            else:
                # Création d'un nouveau (ex: les 10 manquants)
                print(f"Création de {new_min['nameFr']}...")
                new_min['id'] = str(uuid.uuid4())
                new_min['createdAt'] = datetime.utcnow().isoformat() + "Z"
                new_min['updatedAt'] = new_min['createdAt']
                mineral_map[key] = new_min

        # 3. Reconstitution de la liste
        final_list = list(mineral_map.values())
        
        # Tri alphabétique
        final_list.sort(key=lambda x: x['nameFr'])
        
        # Structure finale
        output_data = {
            "version": "6.0",
            "source": "MineraLog v3.0 - Enriched library (v6)",
            "total_minerals": len(final_list),
            "created_date": datetime.now().strftime("%Y-%m-%d"),
            "minerals": final_list
        }

        with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
            json.dump(output_data, f, ensure_ascii=False, indent=2)
            
        print(f"SUCCÈS : {len(final_list)} minéraux exportés dans {OUTPUT_FILE}")
        print("Les doublons ont été fusionnés, les groupes corrigés et les manquants ajoutés.")

    except Exception as e:
        print(f"ERREUR : {str(e)}")

if __name__ == "__main__":
    clean_and_enrich()
