# Sprint 1 Checklist - Report Core Port

## Scope lock
- [ ] Only report module changes in this sprint
- [ ] No replacement of current notification/chat routes
- [ ] No unrelated refactor

## Code integration tasks
- [ ] Add report activities and adapters
- [ ] Add required layouts and drawables for report screens
- [ ] Register new activities/receiver in manifest (minimal required)

## Data safety tasks
- [ ] Remove hardcoded owner identity values
- [ ] Validate tenant/owner/room mapping from real source
- [ ] Ensure writes follow tenancy path and existing Firestore schema
- [ ] Keep backward compatibility for legacy fields where needed

## Bug prevention tasks
- [ ] Reject action button wired and tested
- [ ] Status transitions verified for owner actions
- [ ] Image-selection behavior resolved:
- [ ] Either persist image references correctly
- [ ] Or disable/hide image UI until upload flow is complete
- [ ] Avoid mutable-list assumptions when combining query results

## Build and verification
- [ ] `./gradlew.bat assembleDebug` successful
- [ ] No new lint/compile errors in touched files

## Manual smoke tests (minimum)
- [ ] Tenant creates report successfully
- [ ] Tenant edits and resubmits rejected report
- [ ] Tenant cancels a pending/in-progress report correctly
- [ ] Owner sees incoming report from correct tenant/room
- [ ] Owner confirms schedule
- [ ] Owner moves issue to in-progress
- [ ] Owner marks issue done
- [ ] Owner rejects with reason and tenant can read reason

## Regression watch
- [ ] Home owner menu still opens existing features correctly
- [ ] Tenant menu notification entry still works as before
- [ ] Existing notification center unread badge still updates

## Done criteria
- [ ] All checklist items passed
- [ ] PR notes include risk + screenshots/video of key flows
- [ ] Ready for review and merge into dev
