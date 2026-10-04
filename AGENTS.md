# android-bridge

Read README.md and both language guides before changing this standalone Android project.
Use JDK 17, the checked-in verified Gradle Wrapper 8.9, Android SDK Platform 35 / Build Tools 35.0.0, and Android API 26+.
Do not depend on a sibling nl2sh checkout or commit IDE files, local.properties, generated outputs, credentials, or signing keys.
Maintain matching docs/zh and docs/en pages when build, release, installation, interface, permissions, or safety behavior changes.
Preserve package names, Binder authority and entrypoint compatibility with nl2sh.
The native nl2sh Security → Confirmation → Execution boundary remains mandatory. Do not widen shell/root-only Binder access or add an approval bypass.
Validate changes with the CI commands in .github/workflows/ci.yml. Document actual results without machine-specific details.
