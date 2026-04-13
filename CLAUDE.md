# QuestKeep — Claude Code Directives

## Project Overview
QuestKeep is an Android app for tabletop RPG (Dungeons & Dragons, Pathfinder, etc.) automation. It helps Game Masters manage campaigns and players while providing tools for character management, real-time chat, and dice rolling.

**Tech Stack:**
- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM + Repository Pattern
- Minimum API: 28 (Android 9.0)
- DI: Hilt
- Async: Kotlin Coroutines + Flow
- Local DB: Room
- Backend: Firebase (Recommended for MVP)

---

## Development Priority Stack

### Phase 1: MVP Foundation (Weeks 1-2)
Complete these before anything else:

1. **Project Setup**
   - Create Kotlin + Jetpack Compose project structure
   - Configure Hilt, Room, Firebase dependencies
   - Set up package structure: auth, ui, data, domain, viewmodel, utils

2. **Authentication System**
   - Firebase Auth integration
   - Email + Password registration & login
   - Google OAuth setup
   - Password validation (min 8 chars, 1 letter, 1 digit)
   - Email verification flow
   - Persist auth token in Datastore

3. **Core Data Models**
   - User (id, email, nickname, avatar_url, role)
   - Campaign (id, name, description, game_system, max_players, gm_id)
   - Character (id, name, hp, armor, stats, campaign_id, player_id)
   - ChatMessage (id, sender_id, campaign_id, content, is_private, created_at)
   - Resource (id, character_id, name, current, max) — HP, Mana, etc.

4. **Navigation Structure**
   - Bottom Navigation Bar (Home / Chat / Group / Menu)
   - Floating Action Button for Dice Roller
   - NavGraph with proper deep linking

5. **Character Sheet UI (MVP Version)**
   - Display: HP (with visual bar), Armor, 6 main stats (Str, Dex, etc.)
   - Quick actions: Long-tap HP to open damage dialog
   - Read character data from Room DB
   - Real-time sync with Firestore (if connected)

6. **Group Chat (Real-time)**
   - Message list with sender name + avatar
   - Input field with send button
   - System messages (player joined, etc.)
   - Real-time updates via Firestore listeners
   - Message caching to Room DB
   - Markdown support: **bold**, *italic*, `code`

7. **Dice Roller (Core Functionality)**
   - Floating button opens modal
   - Input field: parse "2d6+3" format
   - Supported dice: d4, d6, d8, d10, d12, d20, d100
   - "Roll Public" button → sends to chat
   - "Roll Private" button → shows only to player + GM
   - Preset system: save "Attack: d20+5, Damage: d8+3"
   - Dice animation on result

8. **Campaign Management (GM Only)**
   - Create campaign form
   - Generate invite code (6-char alphanumeric)
   - Display list of players in campaign
   - View player characters (read-only for GM in MVP)
   - Send campaign notifications (coming soon)

### Phase 2: v1.0 Full Features (Weeks 3-4)
Build after MVP is solid:

1. **GM Master Notes System**
   - Tree structure: World → City → NPC → Plot
   - Rich text editor (Markdown + images)
   - Private notes (never shown to players)
   - "Reveal to players" feature for specific notes

2. **Hidden Stats (GM Feature)**
   - GM can add stats player can't see
   - Mark stat as "revealed" to show to player
   - Examples: Curse, Infection, Transformation

3. **XP System**
   - GM awards XP individually or to whole group
   - Track XP history
   - Auto-level calculation (if rules support)

4. **Private Messages ("Whispers")**
   - Only GM and target player see message
   - System message "GM whispered to you"
   - Can include dice roll results or hints

5. **Bestiary (NPC/Monster Library)**
   - Create NPC cards: name, description, stats
   - Quick insert into chat or notes
   - Campaign-scoped library
   - Reuse across sessions

6. **Skill Tree / Abilities**
   - Hierarchical display (Ability → Sub-ability → Specialization)
   - Icons for each skill
   - "Upgrade" button to level up
   - Full descriptions with effects

7. **Push Notifications**
   - Campaign starting soon (24h, 1h before)
   - Private message received
   - "Quiet hours" setting (e.g., 11 PM - 8 AM)

8. **PDF Rules Reference**
   - Store PDF in Firestore / Cloud Storage
   - In-app PDF viewer (PDFView library)
   - Bookmarks and search in PDF

### Phase 3: v2.0 Nice-to-Have (Future)
Only if time permits:

- Combat Tracker (initiative system)
- Inventory with weight calculation
- Fog of War (reveal/hide map areas)
- Soundboard (background music + SFX)
- Name/Loot/NPC generators
- Full offline sync
- VTT integration

---

## Code Style & Architecture Rules

### Kotlin Idioms
- Use `data class` for models
- Extension functions for repeated logic
- Prefer `sealed class` for error handling
- Use `by lazy` for expensive singletons
- Coroutine-first async (no RxJava)

### Jetpack Compose Patterns
- One Composable = one responsibility
- Use `remember` for local state, not global
- Extract large Composables into separate functions
- Preview annotations for all non-trivial UI
- Pass callbacks, not ViewModels, to leaf Composables

### Architecture Layers
```
UI Layer (Composables + ViewModels)
    ↓
Domain Layer (Use Cases, Entities)
    ↓
Data Layer (Repositories, Room DB, Firebase)
```

**Repository Pattern:** Each entity has a repository that abstracts data sources.

```kotlin
interface CharacterRepository {
    fun getCharacter(id: String): Flow<Character>
    suspend fun updateCharacter(character: Character)
    suspend fun deleteCharacter(id: String)
}

class CharacterRepositoryImpl(
    private val roomDao: CharacterDao,
    private val firestore: FirebaseFirestore
) : CharacterRepository {
    // Implementation with local-first, cloud sync
}
```

### Error Handling
Use custom Result type instead of exceptions:

```kotlin
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error<T>(val exception: Exception) : Result<T>
    class Loading<T> : Result<T>
}
```

### Navigation
- Single NavGraph in main activity
- Deep links for direct campaign/character access
- Arguments via Parcelable (not Serializable)

```kotlin
@Parcelize
data class CampaignScreenArgs(
    val campaignId: String,
    val playerRole: UserRole
) : Parcelable
```

### Testing Requirements
- **Unit Tests:** ViewModels, UseCases, Repositories (local mocking)
- **UI Tests:** Critical Composables (Dice Roller, Chat input)
- **Integration Tests:** Firebase connectivity (use emulator)
- Minimum coverage: 60% for critical paths
- Use MockK for mocking, Turbine for Flow testing

---

## Firebase Setup (Recommended)

### Authentication
- Enable Email/Password provider
- Enable Google OAuth (provide SHA-1 hash)
- Custom claims for user roles (GM vs Player)

### Firestore Collections
```
users/
  {userId}
    - email
    - nickname
    - avatar_url
    - created_at

campaigns/
  {campaignId}
    - name, description, game_system
    - gm_id (admin)
    - invite_code (unique, TTL)
    - max_players
    - created_at

characters/
  {campaignId}_{characterId}
    - player_id
    - name, class, level
    - stats (hp, armor, abilities)
    - resources (mana, stamina)
    - hidden_stats (only visible if revealed=true)

messages/
  {campaignId}_{messageId}
    - sender_id, sender_name
    - content, created_at
    - type (normal, system, whisper)
    - is_public (true = all see, false = only sender + GM)
```

### Firestore Security Rules
```firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if request.auth.uid == userId;
      allow write: if request.auth.uid == userId;
    }
    match /campaigns/{campaignId} {
      allow read: if is_player_in_campaign(campaignId);
      allow create: if request.auth.uid != null;
      allow write: if is_gm(campaignId);
    }
    // More rules as needed...
  }
}
```

---

## Before Each Commit

### Code Quality Checklist
- [ ] No hardcoded strings (use strings.xml or compose resources)
- [ ] All error paths handled (no bare `throw` without catching)
- [ ] Proper resource cleanup (close DB, unsubscribe Flows)
- [ ] No sensitive data logged (no passwords, tokens in logs)
- [ ] Comments for non-obvious logic
- [ ] Function names describe what they do

### UI Checklist
- [ ] Dark theme tested (not just default)
- [ ] Text readable (good contrast)
- [ ] Touch targets ≥48dp
- [ ] No hardcoded colors (use theme colors)
- [ ] Preview annotations added to new Composables

### Testing Checklist
- [ ] Critical ViewModels have unit tests
- [ ] Firebase mocked in repository tests
- [ ] UI tests for user flows (login → create character → send message)
- [ ] No test depends on external services (mock everything)

---

## Known Constraints

### Performance
- Real-time chat sync for 50+ messages/minute may need pagination
- WebSocket/Firestore listeners auto-stop when fragment is paused
- Image compression before uploading to Firebase (max 5MB)

### Security
- Never store auth tokens locally (use Datastore with `EncryptedSharedPreferences`)
- Validate all user input (no SQL injection, HTML injection in chat)
- Sanitize Markdown: only allow **bold**, *italic*, `code`, no scripts
- Hide GM-only data server-side (don't just hide in UI)

### Offline Support
- MVP doesn't need full offline. Cache just the character sheet.
- Unsent messages queue in Room DB, send when reconnected.
- Old chat history available locally (sync last 100 messages).

---

## Useful Commands for Claude Code

```bash
# Start development
./gradlew build
./gradlew installDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Format code
./gradlew ktlintFormat

# Check lint
./gradlew lintDebug
```

---

## File Structure Template

```
questkeep/
├── app/src/main/
│   ├── kotlin/com/questkeep/
│   │   ├── MainActivity.kt
│   │   ├── QuestKeepApp.kt (Hilt setup)
│   │   ├── auth/
│   │   │   ├── AuthViewModel.kt
│   │   │   ├── LoginScreen.kt
│   │   │   └── RegisterScreen.kt
│   │   ├── ui/
│   │   │   ├── screens/
│   │   │   │   ├── CharacterSheetScreen.kt
│   │   │   │   ├── ChatScreen.kt
│   │   │   │   ├── DiceRollerScreen.kt
│   │   │   │   └── CampaignListScreen.kt
│   │   │   └── components/
│   │   │       ├── DiceRollerModal.kt
│   │   │       ├── StatBar.kt
│   │   │       └── ChatMessage.kt
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── database/
│   │   │   │   │   ├── CharacterDao.kt
│   │   │   │   │   └── MessageDao.kt
│   │   │   │   └── QuestKeepDatabase.kt
│   │   │   ├── remote/
│   │   │   │   └── FirebaseService.kt
│   │   │   └── repository/
│   │   │       ├── CharacterRepository.kt
│   │   │       ├── CampaignRepository.kt
│   │   │       └── MessageRepository.kt
│   │   ├── domain/
│   │   │   ├── entities/
│   │   │   │   ├── User.kt
│   │   │   │   ├── Character.kt
│   │   │   │   └── Campaign.kt
│   │   │   └── usecases/
│   │   │       ├── GetCharacterUseCase.kt
│   │   │       └── SendMessageUseCase.kt
│   │   ├── viewmodel/
│   │   │   ├── CharacterSheetViewModel.kt
│   │   │   ├── ChatViewModel.kt
│   │   │   └── DiceRollerViewModel.kt
│   │   └── utils/
│   │       ├── Constants.kt
│   │       ├── Extensions.kt
│   │       └── DiceParser.kt
│   └── res/
│       ├── values/
│       │   ├── colors.xml
│       │   └── strings.xml
│       └── drawable/
├── build.gradle.kts (app-level)
└── settings.gradle.kts
```

---

## Questions to Clarify During Development

1. **Game System:** Which tabletop system is the MVP for? (D&D 5e, Pathfinder, generic stats?)
   - This affects character sheet fields and rules calculations

2. **Character Avatar:** Photo or emoji-based?
   - Photo upload (Firebase Storage + compression)
   - Emoji picker (simpler, no storage)

3. **Notifications:** Push only, or also in-app alerts?
   - Firebase Cloud Messaging (FCM) for push
   - Local notifications for when app is open

4. **Dice Animations:** Simple flip, or fancy 3D animation?
   - Simple (MVP): just show result with brief animation
   - Fancy: use Compose animation API or 3D library (add later)

---

## When You're Stuck

1. **Firebase connection fails:** Check SHA-1 hash in Firebase console matches your signing key
2. **Compose recomposition issues:** Use `.key()` to force recomposition or `mutableStateOf` for local state
3. **Room DB migration:** Always increment DB version and provide migration paths
4. **Network timeouts:** Add retry logic with exponential backoff in repositories
5. **Memory leaks:** Unsubscribe from Flows in `onCleared()` of ViewModel

---

## Success Criteria for MVP Release

- ✅ Users can register, login, create a campaign
- ✅ GM can invite players, view their characters
- ✅ Players can view/edit their character sheet
- ✅ All can send messages in group chat
- ✅ Dice roller works (public + private)
- ✅ Data syncs in real-time between users
- ✅ App doesn't crash under normal use
- ✅ Dark theme applied consistently
- ✅ No hardcoded strings or secrets
- ✅ Ready to publish on Play Store

---

**Last Updated:** February 28, 2026  
**Status:** Ready for development  
**Next Step:** Create the Kotlin + Compose project and start Phase 1 MVP
