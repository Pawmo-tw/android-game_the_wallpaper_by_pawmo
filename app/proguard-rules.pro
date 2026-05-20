# 保護動態桌布服務類別
-keep class pawmo.yee.wallpaper.DinoWallpaperService { *; }

# 保護所有繼承自 WallpaperService 的子類別與內部類別
-keep class * extends android.service.wallpaper.WallpaperService { *; }
-keep class * extends android.service.wallpaper.WallpaperService$Engine { *; }

# 保護繪圖渲染邏輯
-keep class pawmo.yee.wallpaper.CrossyLogic { *; }