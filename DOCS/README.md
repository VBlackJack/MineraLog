# MineraLog Documentation Index

**Version**: 3.0.0-alpha | **Last Updated**: 2025-11-17

Welcome to the MineraLog documentation. This index helps you find the right documentation for your needs.

---

## 📚 Quick Navigation

### For Users

- **[User Guide](user_guide.md)** - Complete guide to using MineraLog (v3.0.0-alpha)
- **[Main README](../README.md)** - Project overview, installation, quick start (FR/EN)
- **[Import/Export Spec](specs/import_export_spec.md)** - CSV and ZIP format specifications

### For Developers

- **[Developer Guide](developer-guide.md)** - Setup, building, testing, debugging
- **[Contributing Guidelines](../CONTRIBUTING.md)** - How to contribute to the project
- **[Architecture Overview](architecture/overview.md)** - Technical architecture, design patterns
- **[ADR: Assumptions](adr/assumptions.md)** - Architecture decision records and implementation rationale

### For QA / Testers

- **[Manual Testing Guide](qa/manual-testing-guide.md)** - Comprehensive QA checklist
- **[TalkBack Testing Checklist](qa/talkback-checklist.md)** - Accessibility testing guide
- **[Accessibility Audit v3.0.0](qa/ACCESSIBILITY_AUDIT_v1.5.0.md)** - WCAG 2.1 AA compliance audit

### Operational

- **[CI Debugging Guide](playbooks/ci-debugging-guide.md)** - Troubleshooting CI/CD failures
- **[Roadmap](roadmap.md)** - Product roadmap and upcoming features

---

## 📁 Documentation Structure

```
docs/
├── README.md                    # This index
├── user_guide.md                # User-facing guide (v3.0.0-alpha)
├── developer-guide.md           # Developer setup & workflow
│
├── architecture/                # Technical architecture
│   └── overview.md              # Clean Architecture, design patterns
│
├── specs/                       # Specifications
│   └── import_export_spec.md    # CSV/ZIP format specs
│
├── adr/                         # Architecture Decision Records
│   └── assumptions.md           # Implementation decisions & rationale
│
├── qa/                          # Quality Assurance
│   ├── manual-testing-guide.md  # Manual QA checklist
│   ├── talkback-checklist.md    # Accessibility testing
│   └── ACCESSIBILITY_AUDIT_v1.5.0.md # WCAG audit
│
├── roadmap/                     # Planning
│   └── roadmap.md               # Product roadmap
│
├── playbooks/                   # Operational guides
│   └── ci-debugging-guide.md    # CI troubleshooting
│
└── _archive/                    # Historical documents
    ├── 2025-11/                 # Session summaries
    ├── sprints/                 # Sprint reports
    ├── releases/                # Release summaries
    ├── bugfixes/                # Bug postmortems
    ├── audits/                  # Old audits
    └── qa/                      # Historical QA reports
```

---

## 🎯 Documentation by Task

### I want to...

**...use MineraLog**
→ Start with [Main README](../README.md) then [User Guide](user_guide.md)

**...contribute code**
→ Read [Contributing Guidelines](../CONTRIBUTING.md) then [Developer Guide](developer-guide.md)

**...understand the architecture**
→ See [Architecture Overview](architecture/overview.md) and [ADR: Assumptions](adr/assumptions.md)

**...import/export data**
→ Check [Import/Export Spec](specs/import_export_spec.md)

**...test accessibility**
→ Use [TalkBack Checklist](qa/talkback-checklist.md)

**...fix a CI issue**
→ Follow [CI Debugging Guide](playbooks/ci-debugging-guide.md)

**...see what's coming next**
→ View [Roadmap](roadmap.md)

---

## 📝 Documentation Standards

### Style Guide

- **Language**: Plain language for user docs, technical precision for dev docs
- **Markdown**: ATX headers (`##`), 120 char lines (soft limit), no hard tabs
- **Links**: Use relative paths, check with `markdown-link-check`
- **Images**: Optimize <100KB, WebP preferred
- **Code blocks**: Always specify language for syntax highlighting

### Front Matter (Active Docs)

All active documentation should include YAML front matter:

```yaml
---
lastReviewed: YYYY-MM-DD
owner: @username
status: active  # active | deprecated | archived
version: 3.0.0-alpha  # App version this doc applies to
---
```

### Review Cadence

| Category | Review Frequency |
|----------|------------------|
| User/Developer guides | Every 6 months OR with minor releases |
| Specs | Every 3 months OR with feature changes |
| Architecture/ADR | Annually OR with major refactors |
| QA checklists | Every 3 months OR with new features |
| Roadmap | Monthly |

---

## 🔍 Finding Archived Documents

Historical documents are in [`_archive/`](_archive/) organized by:
- **Date** (`2025-11/`) - Session summaries, implementation reports
- **Sprints** (`sprints/M1/`, `sprints/M2/`, `sprints/RC/`) - Sprint reports
- **Releases** (`releases/`) - Release summaries (v1.2.0, v1.5.0)
- **Bugfixes** (`bugfixes/`) - Bug postmortems
- **Audits** (`audits/`) - Old accessibility/UX audits
- **QA** (`qa/`) - Historical QA/testing reports

**Searching archives**:
```bash
# Search for a keyword in archived docs
grep -r "keyword" docs/_archive/

# List all archived sprint reports
ls docs/_archive/sprints/
```

---

## 🛠️ Tools & Automation

### Linting

```bash
# Markdown lint
markdownlint docs/**/*.md *.md

# Link checking
markdown-link-check docs/**/*.md
```

### CI/CD

- **[GitHub Actions](.github/workflows/)** - Automated builds, tests, linting
- **CODEOWNERS** - Automatic PR review assignment

### Templates

- **[Issue Templates](.github/ISSUE_TEMPLATE/)** - Bug reports, feature requests, questions
- **[PR Template](.github/PULL_REQUEST_TEMPLATE.md)** - PR checklist

---

## 📞 Get Help

- **Questions**: [GitHub Discussions](https://github.com/VBlackJack/MineraLog/discussions)
- **Bugs**: [GitHub Issues](https://github.com/VBlackJack/MineraLog/issues)
- **Contributions**: See [CONTRIBUTING.md](../CONTRIBUTING.md)

---

## 📜 License

All documentation is licensed under [Apache 2.0](../LICENSE), same as the code.

**Author**: Julien Bombled
**Maintainer**: @VBlackJack
