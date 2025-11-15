# Contributing to MineraLog

Thank you for your interest in contributing to MineraLog! This document provides guidelines and instructions for contributing to the project.

## 🌍 Language / Langue

This project is bilingual (English/Français). Documentation and UI should support both languages.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Code Style](#code-style)
- [Commit Conventions](#commit-conventions)
- [Pull Request Process](#pull-request-process)
- [Testing](#testing)
- [Documentation](#documentation)

---

## Code of Conduct

Be respectful, inclusive, and constructive. This is an open-source project built by volunteers for the mineral collecting community.

**Expected behavior:**
- Use welcoming and inclusive language
- Respect differing viewpoints and experiences
- Accept constructive criticism gracefully
- Focus on what's best for the community

**Unacceptable behavior:**
- Harassment, trolling, or discriminatory language
- Publishing others' private information
- Spam or off-topic discussions

---

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/MineraLog.git
   cd MineraLog
   ```
3. **Create a branch** for your contribution:
   ```bash
   git checkout -b feature/my-new-feature
   # or
   git checkout -b fix/bug-description
   ```

---

## Development Setup

See [docs/developer-guide.md](docs/developer-guide.md) for detailed setup instructions including:
- Android Studio configuration
- JDK and SDK requirements
- Building and running tests
- Debugging tips

**Quick setup:**
```bash
# 1. Install prerequisites
# - Android Studio Ladybug or later
# - JDK 17
# - Android SDK 35

# 2. Create local.properties with Maps API key
echo "MAPS_API_KEY=your_key_here" > local.properties

# 3. Build
./gradlew assembleDebug

# 4. Run tests
./gradlew testDebugUnitTest
```

---

## How to Contribute

### Reporting Bugs

Use the [Bug Report template](.github/ISSUE_TEMPLATE/bug_report.yml):
- **Describe the bug** clearly with steps to reproduce
- **Include device info**: Android version, device model
- **Attach logs** if available (logcat output)
- **Screenshots/videos** help immensely

### Suggesting Features

Use the [Feature Request template](.github/ISSUE_TEMPLATE/feature_request.yml):
- **Explain the use case** (why is this needed?)
- **Describe the solution** you'd like to see
- **Consider alternatives** and trade-offs
- **Check existing issues** to avoid duplicates

### Asking Questions

Use the [Question template](.github/ISSUE_TEMPLATE/question.md) or [GitHub Discussions](https://github.com/VBlackJack/MineraLog/discussions).

---

## Code Style

### Kotlin

We follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- **4 spaces indentation** (no tabs)
- **120 character line limit** (soft limit, 140 hard limit)
- **Use trailing commas** in multiline lists
- **Prefer `val` over `var`** (immutability)
- **Use explicit types** when clarity improves

**Example:**
```kotlin
// ✅ Good
class MineralViewModel(
    private val repository: MineralRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()
}

// ❌ Avoid
class MineralViewModel(private val repository: MineralRepository, private val savedStateHandle: SavedStateHandle): ViewModel() {
    var state = MutableStateFlow<UiState>(UiState.Loading)
}
```

### Compose UI

- **Prefer stateless composables** (hoist state when possible)
- **Use `remember` for expensive operations**
- **Extract reusable composables** (DRY principle)
- **Accessibility**: Always provide `contentDescription` for icons/images

### File Organization

```
app/src/main/java/net/meshcore/mineralog/
├── data/           # Data layer (Room, repositories, services)
├── ui/             # Presentation layer (Compose screens, ViewModels)
│   ├── screens/    # Full screens
│   ├── components/ # Reusable UI components
│   └── navigation/ # Navigation logic
└── util/           # Utilities, extensions
```

---

## Commit Conventions

We use [Conventional Commits](https://www.conventionalcommits.org/):

**Format:**
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style (formatting, no logic change)
- `refactor`: Code refactoring (no feature/bug change)
- `test`: Adding/updating tests
- `chore`: Maintenance (dependencies, build config)
- `perf`: Performance improvement
- `ci`: CI/CD changes

**Scope** (optional): `database`, `ui`, `backup`, `camera`, `i18n`, etc.

**Examples:**
```bash
feat(camera): add UV photo type support
fix(database): resolve foreign key cascade issue
docs(readme): update installation instructions
refactor(ui): extract photo gallery component
test(repository): add backup encryption tests
```

**Rules:**
- Subject: imperative mood ("add" not "added"), max 72 chars
- Body: explain **why**, not **what** (code shows what)
- Footer: reference issues (`Fixes #123`, `Closes #456`)

---

## Pull Request Process

### Before Submitting

1. **Update from main:**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Run tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew connectedDebugAndroidTest  # If device connected
   ```

3. **Check lint:**
   ```bash
   ./gradlew lint
   ```

4. **Build successfully:**
   ```bash
   ./gradlew assembleDebug
   ```

### PR Checklist

When opening a PR, ensure:

- [ ] Code follows Kotlin style guide
- [ ] All tests pass (`./gradlew test`)
- [ ] New code has test coverage (aim for >80% on new files)
- [ ] Documentation updated (if changing APIs/features)
- [ ] CHANGELOG.md updated (if user-facing change)
- [ ] Commit messages follow conventions
- [ ] PR description explains **why** (not just what)
- [ ] Screenshots/videos attached (if UI change)
- [ ] Accessibility tested (TalkBack if applicable)
- [ ] No breaking changes (or clearly documented)

### Review Process

1. **Automated checks** must pass (CI builds, tests, lint)
2. **Maintainer review** (typically within 3-7 days)
3. **Address feedback** with new commits (don't force-push during review)
4. **Squash if requested** before merge

---

## Testing

### Unit Tests

- **Location**: `app/src/test/`
- **Framework**: JUnit 5
- **Run**: `./gradlew testDebugUnitTest`

**Example:**
```kotlin
@Test
fun `importMineral should save to database`() = runTest {
    // Given
    val mineral = TestFixtures.createMineral(name = "Quartz")

    // When
    repository.insertMineral(mineral)

    // Then
    val result = repository.getMineralById(mineral.id)
    assertThat(result).isEqualTo(mineral)
}
```

### UI Tests

- **Location**: `app/src/androidTest/`
- **Framework**: Espresso + Compose Testing
- **Run**: `./gradlew connectedDebugAndroidTest` (requires device/emulator)

### Manual Testing

See [docs/qa/manual-testing-guide.md](docs/qa/manual-testing-guide.md) for comprehensive checklist.

**Critical flows to test:**
- Add/edit/delete mineral
- Take photo (all 4 types)
- Import/export (ZIP + CSV)
- Search and filters
- Accessibility (TalkBack navigation)

---

## Documentation

### When to Update Docs

Update documentation when you:
- Add a new feature → Update `docs/user-guide.md` + `CHANGELOG.md`
- Change architecture → Update `docs/architecture/overview.md`
- Modify APIs/specs → Update relevant `docs/specs/*.md`
- Fix a bug → Add note in `CHANGELOG.md`

### Documentation Style

- **User-facing docs** (README, user-guide): Plain language (B1 French / Plain English)
- **Technical docs** (architecture, specs): Precise, with code examples
- **Keep it DRY**: Link to authoritative source instead of duplicating

### Markdown Guidelines

- Use ATX-style headers (`##` not underlines)
- Max 120 chars per line (soft limit)
- Add table of contents for docs >5 sections
- Use fenced code blocks with language specifiers
- Optimize images (WebP preferred, <100KB each)

---

## Questions?

- **General questions**: [GitHub Discussions](https://github.com/VBlackJack/MineraLog/discussions)
- **Bug reports**: [GitHub Issues](https://github.com/VBlackJack/MineraLog/issues)
- **Security vulnerabilities**: Email maintainer privately (see SECURITY.md)

---

## License

By contributing, you agree that your contributions will be licensed under the [Apache 2.0 License](LICENSE).

---

**Thank you for contributing to MineraLog!** 🪨⛏️
