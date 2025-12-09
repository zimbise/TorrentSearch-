#!/bin/bash
# Commit and push all restriction removal changes

cd /workspaces/TorrentSearch-

echo "📝 Staging changes..."
git add -A

echo "📦 Committing changes..."
git commit -m "remove: eliminate all default content restrictions for clean custom logic

- Disabled NSFW mode filtering (default true, always show content)
- Removed NSFW filter from search results, bookmarks, and UI
- Removed Safe Mode toggle from settings screen
- Removed disableRestrictedSearchProviders() function
- All providers enabled by default regardless of safety status
- No built-in censorship, flags, or blockers remaining
- Metadata still available for custom restriction implementation
- Clean slate for aggressive custom content configuration

This allows custom restrictions to be implemented without conflicts
from the app's default filtering logic."

echo "🚀 Pushing to GitHub (triggers GitHub Actions)..."
git push origin main

echo ""
echo "✅ Done! GitHub Actions will now build the unrestricted APK"
echo "📥 Download when ready: https://github.com/prajwalch/TorrentSearch/releases/tag/latest"
