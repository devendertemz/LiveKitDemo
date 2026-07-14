# LiveKitDemo

A LiveKit-based video calling demo for Android, built with Kotlin, XML Views, and an MVVM architecture, paired with a minimal Node.js token server.

### Application Side

#### LiveKit SDK integration
- Uses `io.livekit:livekit-android` (version `2.27.0`, declared in `gradle/libs.versions.toml`) as the sole video/audio SDK dependency.
- Resolved via Maven Central + JitPack (JitPack added to `settings.gradle.kts` as required by the SDK).
- Kotlin sources compiled via AGP 9's built-in Kotlin support (no separate `org.jetbrains.kotlin.android` plugin needed).
- `buildFeatures.viewBinding = true` is enabled; the UI is built with XML layouts + ViewBinding, not Compose.

#### Room connection flow
1. `HomeActivity` collects three fields: **Token Server** URL, **Your Name** (identity), and **Room Name**.
2. On Join, `PermissionHelper` verifies camera/microphone/Bluetooth permissions are granted.
3. `HomeActivity` calls `TokenApiClient.fetchToken(tokenServer, room, identity)`, which does a plain `HttpURLConnection` GET to `<tokenServer>/token?room=...&identity=...` and parses the JSON response (`token`, `serverUrl`).
4. `HomeActivity` starts `CallActivity`, passing `serverUrl`, `token`, and `roomName` as Intent extras.
5. `CallActivity` calls `CallViewModel.connect(serverUrl, token, roomName)`, which delegates to `LiveKitRepository` → `LiveKitManager.connect()`.
6. `LiveKitManager` creates the `Room` via `LiveKit.create(context.applicationContext)` and connects with `room.connect(url, token)`.

#### Audio/Video calling features
- On successful connect, `LiveKitManager.connect()` immediately calls `localParticipant.setMicrophoneEnabled(true)` and `setCameraEnabled(true)`, publishing both local tracks.
- Local camera preview is rendered in a dedicated `SurfaceViewRenderer` (`localVideoRenderer`) in `activity_call.xml`.
- Remote participants' video is rendered in per-item `SurfaceViewRenderer`s inside the participant grid (`item_participant.xml`).
- Speaker output is toggled through `Room.audioHandler` cast to `AudioSwitchHandler`, selecting between `AudioDevice.Speakerphone` and the first non-speakerphone device.
- Front/back camera switching is done via `LocalVideoTrack.switchCamera(position)`, toggling between `CameraPosition.FRONT` and `CameraPosition.BACK`.

#### Participant management
- `RoomListener.snapshotParticipants(room)` rebuilds the full participant list (local participant + all `room.remoteParticipants`, sorted by identity) from the live `Room` object.
- Each `Participant` is mapped to a `ParticipantModel` (`sid`, `identity`, `displayName`, `isLocal`, `videoTrack`, `isMicEnabled`, `isCameraEnabled`, `isSpeaking`).
- `LiveKitManager` re-runs this snapshot on every `RoomEvent`, so the participant list, mute states, and speaking state all stay current.
- `ParticipantAdapter` (a `RecyclerView.ListAdapter` with `DiffUtil`) renders the **remote** participants; the local participant is rendered separately in its own preview view, not in the grid.

#### Camera/Microphone controls
- **Mic button** → `CallViewModel.toggleMic()` → `LocalParticipant.setMicrophoneEnabled(enabled)`.
- **Camera button** → `toggleCamera()` → `setCameraEnabled(enabled)`.
- **Switch-camera button** → `switchCamera()` → `LocalVideoTrack.switchCamera(position)`.
- **Speaker button** → `toggleSpeaker()` → `AudioSwitchHandler.selectDevice(...)`.
- **Leave button** → `CallViewModel.leave()` (disconnects the room) then finishes `CallActivity`.
- Button background tint switches between a neutral gray (enabled) and red (muted/off/leave) to reflect state.

#### LiveKit events handled
- `LiveKitManager` subscribes to `room.events` (the SDK's `RoomEvent` flow) generically — **every** event (participant joined/left, track published/subscribed/unpublished, mute/unmute, active-speaker changes, etc.) triggers the same handler, which re-runs `RoomListener.snapshotParticipants(room)` to rebuild the participant list from current state. Individual `RoomEvent` subtypes are not pattern-matched separately.
- Connection state (`CONNECTING`, `CONNECTED`, `RECONNECTING`, `DISCONNECTED`) is observed separately via `room::state.flow` and reflected as the status text in `CallActivity` (e.g. "Connecting… · room-name", "Reconnecting… · room-name").

#### UI implementation
- **`HomeActivity`** (`activity_home.xml`): a `ScrollView` with three `TextInputLayout` fields (Token Server, Your Name, Room Name) and a `MaterialButton` to join.
- **`CallActivity`** (`activity_call.xml`): a `ConstraintLayout` containing a status bar `TextView`, a `RecyclerView` (`GridLayoutManager`, 2 columns) for remote participants, a small local-preview `SurfaceViewRenderer` in the top-right corner, and a bottom control bar with 5 `MaterialButton`s (Mic, Cam, Flip, Spkr, Leave).
- **`item_participant.xml`**: a `SurfaceViewRenderer` with an overlay bar showing the participant's name and "MIC OFF" / "CAM OFF" labels when applicable.
- Architecture is MVVM: `CallViewModel` exposes a single `StateFlow<CallUiState>`; `CallActivity` collects it via `repeatOnLifecycle(STARTED)` and re-renders the status text, participant list, local preview, and button tints on every emission.

---

### Backend Side

#### Token generation
- `server/index.js` is a small Express app with one route: `GET /token?room=<room>&identity=<identity>`.
- It builds an `AccessToken` (from `livekit-server-sdk`) using `LIVEKIT_API_KEY` / `LIVEKIT_API_SECRET` from environment variables, sets `identity` and a `ttl` of `6h`, adds a `{ roomJoin: true, room }` grant, and returns the signed JWT via `await at.toJwt()`.
- Response body: `{ "token": "<jwt>", "serverUrl": "<LIVEKIT_URL>" }` — the LiveKit server URL is also served from here so the app doesn't need to hardcode it.
- Credentials and config (`LIVEKIT_URL`, `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`, `PORT`) are loaded from a local `.env` file via `dotenv` and are not present anywhere in the Android app.

#### User authentication
- Not implemented. There is no login, session, or user-identity verification — the `identity` value is whatever plain text the user types into the "Your Name" field on the Home screen, passed straight through to token generation.

#### Room management
- Not implemented. There is no room creation, listing, or deletion logic (no use of LiveKit's `RoomServiceClient`). Rooms are implicitly created by LiveKit Cloud when the first participant joins.

#### LiveKit webhooks
- Not implemented. The server does not expose a webhook receiver endpoint and does not process any LiveKit webhook events.

#### API endpoints used
- `GET /token` — the only HTTP endpoint the server exposes. Query params: `room`, `identity`. Returns `400` with `{ "error": "..." }` if either is missing; otherwise `200` with `{ "token", "serverUrl" }`.

#### Business logic
- Minimal: validates that `room` and `identity` are both present, fixes the token TTL at 6 hours, and otherwise passes straight through to the LiveKit server SDK's token-signing API. No database, persistence, or multi-request state is involved.
