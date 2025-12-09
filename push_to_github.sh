#!/bin/bash
# Push changes to GitHub with proper error handling

set -e

echo "================================"
echo "🚀 Pushing to GitHub"
echo "================================"
echo ""

cd /workspaces/TorrentSearch-

echo "📊 Checking git status..."
git status

echo ""
echo "📤 Attempting to push to origin main..."
git push -v origin main 2>&1 || {
    echo ""
    echo "❌ Push failed. Possible causes:"
    echo ""
    echo "1. GitHub Authentication:"
    echo "   - You may need to authenticate with GitHub"
    echo "   - Use: gh auth login"
    echo "   - Or set up SSH keys"
    echo ""
    echo "2. Check remote URL:"
    echo "   - Current: $(git remote get-url origin)"
    echo "   - Should be: https://github.com/zimbise/TorrentSearch-"
    echo ""
    echo "3. Try authenticating then push again:"
    echo "   - gh auth login"
    echo "   - git push origin main"
    echo ""
    exit 1
}

echo ""
echo "✅ Push successful!"
echo "📥 GitHub Actions will now build the APK"
echo "⏱️  Build takes ~5-10 minutes"
echo "📥 Download: https://github.com/zimbise/TorrentSearch-/releases/tag/latest"
