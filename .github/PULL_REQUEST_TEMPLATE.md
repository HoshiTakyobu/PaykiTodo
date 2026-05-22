## Summary

-

## User-visible changes

-

## Testing

- [ ] `git diff --check`
- [ ] `./gradlew.bat :app:assembleDebug`
- [ ] `./gradlew.bat :app:testDebugUnitTest` if behavior/parser/database logic changed
- [ ] `node --check app/src/main/assets/desktop-web/app.js` if desktop web JavaScript changed

## Data / migration impact

- [ ] No database schema change
- [ ] Database migration added and documented
- [ ] Backup / restore and desktop sync JSON updated if needed

## Privacy and secrets

- [ ] No API keys, tokens, private Base URLs, signing files, backups, generated APKs/AABs, or personal logs are included
- [ ] Screenshots/logs are redacted

## Documentation

- [ ] README / CHANGELOG / docs updated where user-visible behavior changed
