#!/bin/bash

# Quick Fix Script for Android Studio Build Issues
# This script helps when changes aren't visible in the simulator

echo "🔧 Android Studio Quick Fix Script"
echo "=================================="
echo ""

# Check if we're in the right directory
if [ ! -f "settings.gradle.kts" ]; then
    echo "❌ Error: Please run this script from the project root directory"
    echo "   Current directory: $(pwd)"
    exit 1
fi

echo "📍 Project found: $(pwd)"
echo ""

# Function to check if Android Studio is running
check_android_studio() {
    if pgrep -x "Android Studio" > /dev/null; then
        echo "✅ Android Studio is running"
        return 0
    else
        echo "⚠️  Android Studio is not running"
        return 1
    fi
}

# Main menu
echo "What would you like to do?"
echo ""
echo "1. Clean build folders (recommended first step)"
echo "2. Open project in Android Studio"
echo "3. List connected devices/emulators"
echo "4. Uninstall app from all devices"
echo "5. Full clean (delete all build artifacts)"
echo "6. Open troubleshooting guide"
echo ""
read -p "Enter your choice (1-6): " choice

case $choice in
    1)
        echo ""
        echo "🧹 Cleaning build folders..."
        rm -rf composeApp/build
        rm -rf build
        echo "✅ Build folders cleaned"
        echo ""
        echo "Next steps in Android Studio:"
        echo "1. File → Sync Project with Gradle Files"
        echo "2. Build → Rebuild Project"
        echo "3. Run the app"
        ;;
    
    2)
        echo ""
        echo "🚀 Opening project in Android Studio..."
        open -a "Android Studio" .
        ;;
    
    3)
        echo ""
        echo "📱 Checking for connected devices..."
        if command -v adb &> /dev/null; then
            adb devices -l
        else
            echo "❌ ADB not found. Make sure Android SDK is installed."
        fi
        ;;
    
    4)
        echo ""
        echo "🗑️  Uninstalling app from all devices..."
        if command -v adb &> /dev/null; then
            adb uninstall com.example.sitacardent
            echo "✅ App uninstalled (if it was installed)"
        else
            echo "❌ ADB not found. Please uninstall manually from the simulator."
        fi
        ;;
    
    5)
        echo ""
        echo "🧹 Performing full clean..."
        echo "This will delete:"
        echo "  - All build folders"
        echo "  - Gradle cache"
        echo "  - IDE caches"
        echo ""
        read -p "Are you sure? (y/n): " confirm
        if [ "$confirm" = "y" ]; then
            rm -rf composeApp/build
            rm -rf build
            rm -rf .gradle
            rm -rf .idea/caches
            echo "✅ Full clean completed"
            echo ""
            echo "Next steps:"
            echo "1. Restart Android Studio"
            echo "2. Wait for Gradle sync"
            echo "3. Build → Rebuild Project"
        else
            echo "Cancelled."
        fi
        ;;
    
    6)
        echo ""
        echo "📖 Opening troubleshooting guide..."
        if [ -f "$HOME/.gemini/antigravity/brain/b165f865-35db-411a-8041-a0b24321d3d1/troubleshooting_guide.md" ]; then
            open "$HOME/.gemini/antigravity/brain/b165f865-35db-411a-8041-a0b24321d3d1/troubleshooting_guide.md"
        else
            echo "Guide not found. Check the artifacts folder."
        fi
        ;;
    
    *)
        echo "Invalid choice"
        exit 1
        ;;
esac

echo ""
echo "✨ Done!"
