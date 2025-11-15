#!/bin/bash
set -e

echo "🔄 MineraLog Documentation Restructure Script"
echo "=============================================="
echo ""

# P1.1: Rename DOCS → docs (lowercase)
echo "📁 Step 1/5: Renaming DOCS → docs..."
if [ -d "DOCS" ]; then
    git mv DOCS docs
    echo "   ✅ DOCS renamed to docs"
else
    echo "   ⚠️  DOCS already renamed or not found"
fi

# P1.2: Create target structure
echo ""
echo "📁 Step 2/5: Creating target folder structure..."
mkdir -p docs/{architecture,specs,adr,qa,roadmap,playbooks}
mkdir -p docs/_archive/{2025-11,sprints/{M1,M2,RC},releases,bugfixes,audits,qa}
echo "   ✅ Folder structure created"

# P1.3: Archive dated files
echo ""
echo "📦 Step 3/5: Archiving dated session/sprint/bugfix reports..."

# Session summaries
if [ -f "docs/SESSION_SUMMARY_2025-11-15.md" ]; then
    git mv docs/SESSION_SUMMARY_2025-11-15.md docs/_archive/2025-11/
fi
if [ -f "docs/SESSION_HANDOFF.md" ]; then
    git mv docs/SESSION_HANDOFF.md docs/_archive/2025-11/
fi
if [ -f "docs/IMPLEMENTATION_SUMMARY_2025-11-15.md" ]; then
    git mv docs/IMPLEMENTATION_SUMMARY_2025-11-15.md docs/_archive/2025-11/
fi
if [ -f "docs/ACCESSIBILITY_FIXES_2025-11-15.md" ]; then
    git mv docs/ACCESSIBILITY_FIXES_2025-11-15.md docs/_archive/2025-11/
fi

# Sprint reports
if [ -f "docs/SPRINT_M1_COMPLETION.md" ]; then
    git mv docs/SPRINT_M1_COMPLETION.md docs/_archive/sprints/M1/
fi
if [ -f "docs/SPRINT_M2_COMPLETION.md" ]; then
    git mv docs/SPRINT_M2_COMPLETION.md docs/_archive/sprints/M2/
fi
if [ -f "docs/SPRINT_RC_PROGRESS.md" ]; then
    git mv docs/SPRINT_RC_PROGRESS.md docs/_archive/sprints/RC/
fi
if [ -f "docs/M1_SPRINT_PLAN.md" ]; then
    git mv docs/M1_SPRINT_PLAN.md docs/_archive/sprints/M1/
fi
if [ -f "docs/M1_FINAL_REPORT.md" ]; then
    git mv docs/M1_FINAL_REPORT.md docs/_archive/sprints/M1/
fi
if [ -f "docs/M1_IMPLEMENTATION_SUMMARY.md" ]; then
    git mv docs/M1_IMPLEMENTATION_SUMMARY.md docs/_archive/sprints/M1/
fi
if [ -f "docs/M2_SPRINT_SUMMARY.md" ]; then
    git mv docs/M2_SPRINT_SUMMARY.md docs/_archive/sprints/M2/
fi

# Release summaries
if [ -f "docs/RELEASE_v1.5.0_SUMMARY.md" ]; then
    git mv docs/RELEASE_v1.5.0_SUMMARY.md docs/_archive/releases/
fi
if [ -f "docs/v1.2.0_sprint_summary.md" ]; then
    git mv docs/v1.2.0_sprint_summary.md docs/_archive/releases/
fi

# Bugfix reports
if [ -f "docs/P0_BUG_FIX_DATA_LOSS_2025-11-15.md" ]; then
    git mv docs/P0_BUG_FIX_DATA_LOSS_2025-11-15.md docs/_archive/bugfixes/
fi
if [ -f "docs/P1_BUG_FIX_IMPLEMENTATION_2025-11-15.md" ]; then
    git mv docs/P1_BUG_FIX_IMPLEMENTATION_2025-11-15.md docs/_archive/bugfixes/
fi
if [ -f "docs/BUG_P1_MINERAL_NOT_IN_LIST.md" ]; then
    git mv docs/BUG_P1_MINERAL_NOT_IN_LIST.md docs/_archive/bugfixes/
fi

# QA reports
if [ -f "docs/AUTOMATED_TESTING_REPORT_2025-11-15.md" ]; then
    git mv docs/AUTOMATED_TESTING_REPORT_2025-11-15.md docs/_archive/qa/
fi
if [ -f "docs/QA_FINAL_REPORT.md" ]; then
    git mv docs/QA_FINAL_REPORT.md docs/_archive/qa/
fi
if [ -f "docs/RC_VALIDATION_REPORT.md" ]; then
    git mv docs/RC_VALIDATION_REPORT.md docs/_archive/qa/
fi
if [ -f "docs/COVERAGE_ANALYSIS.md" ]; then
    git mv docs/COVERAGE_ANALYSIS.md docs/_archive/qa/
fi
if [ -f "docs/CI_HEALTH_REPORT.md" ]; then
    git mv docs/CI_HEALTH_REPORT.md docs/_archive/qa/
fi

# Audits
if [ -f "docs/ACCESSIBILITY_AUDIT_REPORT.md" ]; then
    git mv docs/ACCESSIBILITY_AUDIT_REPORT.md docs/_archive/audits/
fi
if [ -f "docs/UX_I18N_ACCESSIBILITY_AUDIT_2025-11-14.md" ]; then
    git mv docs/UX_I18N_ACCESSIBILITY_AUDIT_2025-11-14.md docs/_archive/audits/
fi

# Implementation summaries
if [ -f "docs/QUICK_WINS_IMPLEMENTATION_SUMMARY.md" ]; then
    git mv docs/QUICK_WINS_IMPLEMENTATION_SUMMARY.md docs/_archive/2025-11/
fi

echo "   ✅ Archived 23+ files"

# P1.4: Organize active docs into categories
echo ""
echo "📋 Step 4/5: Organizing active documentation..."

# Specs
if [ -f "docs/import_export_spec.md" ]; then
    git mv docs/import_export_spec.md docs/specs/
fi

# QA (active)
if [ -f "docs/MANUAL_QA_TESTING_GUIDE_v1.5.0.md" ]; then
    git mv docs/MANUAL_QA_TESTING_GUIDE_v1.5.0.md docs/qa/manual-testing-guide.md
fi
if [ -f "docs/TALKBACK_TESTING_CHECKLIST.md" ]; then
    git mv docs/TALKBACK_TESTING_CHECKLIST.md docs/qa/talkback-checklist.md
fi
if [ -f "docs/ACCESSIBILITY_AUDIT_v1.5.0.md" ]; then
    git mv docs/ACCESSIBILITY_AUDIT_v1.5.0.md docs/qa/
fi

# Playbooks
if [ -f "docs/ci-debugging-guide.md" ]; then
    git mv docs/ci-debugging-guide.md docs/playbooks/
fi

# ADR
if [ -f "docs/assumptions.md" ]; then
    git mv docs/assumptions.md docs/adr/
fi

echo "   ✅ Organized active docs into categories"

# P1.5: Move architecture docs
echo ""
echo "🏗️  Step 5/5: Moving architecture documentation..."
if [ -f "ARCHITECTURE.md" ]; then
    git mv ARCHITECTURE.md docs/architecture/overview.md
    echo "   ✅ Moved ARCHITECTURE.md → docs/architecture/overview.md"
fi

echo ""
echo "✅ Documentation restructure complete!"
echo ""
echo "📊 Summary:"
echo "   - DOCS → docs (lowercase)"
echo "   - 23+ files archived to docs/_archive/"
echo "   - Active docs organized into: specs/, qa/, playbooks/, adr/, architecture/"
echo "   - Folder structure: docs/{architecture,specs,adr,qa,roadmap,playbooks,_archive}"
echo ""
echo "Next: Run 'tree docs' to view new structure"
