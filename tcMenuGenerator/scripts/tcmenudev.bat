@echo off
setlocal
set MY_PATH=%~dp0
echo TcMenu developer version from %MY_PATH%
java --module-path %MY_PATH%/../target/jfx/deps -Doverride.core.plugin.dir=%MY_PATH%\..\target\jfx\app "-Dprism.lcdtext=false" --add-modules com.thecoderscorner.tcmenu.menuEditorUI com.thecoderscorner.menu.editorui.cli.TcMenuDesignerCmd %*
