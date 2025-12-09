Perfect — let’s take the **entire TorrentSearch technical specification** and reformat it into clean Markdown, end‑to‑end. This way you can drop it straight into GitHub Codespaces as `SPEC.md` or similar. I’ll keep everything structured, developer‑friendly, and polished.

---

# TorrentSearch – Full Technical Specification

## 1. High‑Level Architecture
- **App name:** TorrentSearch (Android)  
- **Platform:** Android, optimized for devices like Samsung A32  
- **Role:** Local‑network torrent meta‑search client  
- **Backend dependency:** User’s own **Jackett** and/or **Prowlarr** instance, hosted on their computer/server, reachable via LAN IP  
- **Trust model:** The app assumes the user is responsible for their own indexers, content, and filters. No hard‑coded censorship, content filters, or “safe mode” behaviors.

**Core idea:**  
TorrentSearch is a frontend UI that queries Jackett/Prowlarr Torznab endpoints, merges and displays results, and exposes magnet links (and optionally `.torrent` URLs) to the user. The app does not scrape sites directly; it simply uses the user’s own providers via API.

---

## 2. Network and Host Configuration
- **Host:** The user’s computer (or server) runs Jackett and/or Prowlarr.  
- **Base URL format (canonical):**
  ```
  http://192.168.1.175:9117/
  ```
  Must include:
  - `http://`
  - LAN IP (not `127.0.0.1` and not `localhost`)
  - Port `9117`
  - Trailing slash `/`

- **Jackett endpoint convention (Torznab v2):**
  ```
  http://192.168.1.175:9117/api/v2.0/indexers/{indexer-name}/results/torznab/
  ```

- **Future flexibility:**
  - Base IP and port must be editable in settings (e.g., switching from `192.168.1.175` to another host).  
  - Support for Jackett and Prowlarr handled via URL patterns and configuration, not hard‑coded to one tool.

---

## 3. Provider System

### 3.1 Provider Definition
Each provider is represented internally as an object with at least:

- **id:** Stable identifier (e.g., `"1337x"`, `"thepiratebay"`, `"rutracker-ru"`)  
- **name:** User‑facing name (e.g., *1337x*, *The Pirate Bay*)  
- **enabled:** Boolean flag for whether the provider is active  
- **apiUrl:** Full Torznab endpoint URL  
- **apiKey:** Optional, if required by the provider  
- **categories:** Supported categories (movies, TV, music, etc.)  

### 3.2 Provider Management
- Providers are stored in a local database or config file.  
- Users can add, edit, or remove providers via the app’s settings.  
- Providers must be validated (ping endpoint, check API key).  
- Disabled providers are skipped during searches.

---

## 4. Search System

### 4.1 Query Handling
- User enters a search term in the app.  
- App sends parallel requests to all enabled providers.  
- Each request uses Torznab query format:
  ```
  ?apikey={key}&t=search&cat={category}&q={query}
  ```

### 4.2 Result Merging
- Results from all providers are merged into a single list.  
- Duplicate detection based on title + size + provider.  
- Sorting options:
  - By relevance  
  - By seeders/leechers  
  - By size  
  - By provider  

### 4.3 Result Display
- Show title, size, seeders/leechers, provider name.  
- Tap → copy magnet link or open in torrent client.  
- Long‑press → additional options (copy `.torrent` URL, share, etc.).

---

## 5. Magnet and Torrent Handling
- **Magnet links:** Exposed directly in results.  
- **Torrent files:** If provider supports `.torrent` URLs, allow download.  
- **Integration:** Optionally integrate with installed torrent clients via Android intent.

---

## 6. Settings and Configuration
- **Host/IP:** Editable base URL for Jackett/Prowlarr.  
- **Port:** Editable port (default `9117`).  
- **API key:** Stored securely per provider.  
- **Categories:** User can enable/disable categories globally or per provider.  
- **Theme:** Light/Dark mode.  
- **Cache:** Option to clear cached results.

---

## 7. UI/UX Design
- **Main screen:** Search bar + results list.  
- **Settings screen:** Provider management, host configuration, theme.  
- **Result item layout:**  
  - Title (bold)  
  - Size + seeders/leechers (secondary text)  
  - Provider name (tag)  
- **Actions:** Tap → magnet, Long‑press → menu.

---

## 8. Future Flexibility
- Support for multiple backend hosts (Jackett + Prowlarr simultaneously).  
- Support for category browsing (not just search).  
- Offline caching of recent searches.  
- Export/import provider configs.  
- Optional authentication for remote servers.

---

## 9. Trust and Responsibility
- TorrentSearch does not enforce content filters.  
- Responsibility for indexers, content, and legality lies with the user.  
- App is purely a frontend for user‑configured APIs.

---

# End of Specification

---

✨ This is now the **complete, polished Markdown spec** — ready to save as `SPEC.md` or `TorrentSearch_Spec.md` in your repo.  

Would you like me to also create a **developer quick‑start section** (setup steps, example config, sample API call) so anyone cloning the repo can immediately test TorrentSearch against Jackett/Prowlarr?