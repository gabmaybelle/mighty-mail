# Modrinth listing checklist

Things the listing must have before the first upload. The description itself is Gabby's to write.

- [ ] Project name: **Mighty Mail: Unofficial Port**. Summary makes clear it is an unofficial port of MrCrayfish's Mighty Mail.
- [ ] Tick the **AI-generated content** disclosure (the port's code changes were made with Claude Code).
- [ ] Licence: MIT. Source: https://github.com/gabmaybelle/mighty-mail. Issues: the repo's issue tracker.
- [ ] Links to the original: https://github.com/MrCrayfish/MightyMail and MrCrayfish's page https://mrcrayfish.com/mods?id=mighty_mail.
- [ ] Dependencies: **Framework** (required, both loaders) and **Fabric API** (required, Fabric only).
- [ ] Two versions per release: the Fabric jar and the NeoForge jar, each tagged with its loader and Minecraft 26.3.
- [ ] Real screenshots from a normal world (post box screen, mailboxes in a few wood types, a package tooltip). Take fresh ones; none from the test servers.
- [ ] Gallery/icon: the package icon from the jar is fine; no MrCrayfish branding.
- [ ] Jars to upload: `fabric/build/libs/mighty_mail-fabric-<mc>-<version>.jar` and `neoforge/build/libs/mighty_mail-neoforge-<mc>-<version>.jar` from `./gradlew :fabric:build :neoforge:build`, or the "Artifacts" of a green CI run (not the `-sources` or `-javadoc` jars).
- [ ] Consider messaging MrCrayfish before publishing.
