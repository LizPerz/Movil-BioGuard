import os
import re

# Paths
BASE_DIR = r"c:\Users\alexi\OneDrive\Pictures\POKEMON\EDEL2.0\BioGuardMovil (1)\BioGuardMovil\app\src\main\java\com\bioguard\movil\ui"
SCREENS_DIR = os.path.join(BASE_DIR, "screens")
COMPONENTS_DIR = os.path.join(BASE_DIR, "components")
STRINGS_XML = r"c:\Users\alexi\OneDrive\Pictures\POKEMON\EDEL2.0\BioGuardMovil (1)\BioGuardMovil\app\src\main\res\values\strings.xml"

# List of files in priority order
screens = [
    "RegisterScreen.kt", "OnboardingScreen.kt", "PasswordRecoveryScreen.kt", "LoginScreen.kt", 
    "DashboardScreen.kt", "SupportScreen.kt", "HistoryScreen.kt", "ProfileScreen.kt", 
    "CuidadorScreen.kt", "AlertScreen.kt", "AnalysisScreen.kt", "SettingsScreen.kt", 
    "MedicationScreen.kt", "ReportsScreen.kt", "DeviceScreen.kt", "QrScannerScreen.kt", 
    "NotificationsScreen.kt", "SplashScreen.kt"
]
components = ["NotificationComponents.kt", "BottomNavBar.kt", "ErrorRetryBox.kt", "FormComponents.kt"]

# Regex patterns to find strings inside specific Compose elements
# Matches Text("Some String"
string_pattern = re.compile(r'Text\s*\(\s*text\s*=\s*"([^"]+)"', re.IGNORECASE)
string_pattern_2 = re.compile(r'Text\s*\(\s*"([^"]+)"', re.IGNORECASE)
placeholder_pattern = re.compile(r'placeholder\s*=\s*{\s*Text\s*\(\s*"([^"]+)"', re.IGNORECASE)
label_pattern = re.compile(r'label\s*=\s*{\s*Text\s*\(\s*"([^"]+)"', re.IGNORECASE)
title_pattern = re.compile(r'title\s*=\s*"([^"]+)"', re.IGNORECASE)
message_pattern = re.compile(r'message\s*=\s*"([^"]+)"', re.IGNORECASE)
# More broad regex for anything that looks like user-facing text
broad_text_pattern = re.compile(r'"([^"\\]*?[a-zA-ZáéíóúÁÉÍÓÚñÑ][^"\\]*?)"')

def generate_key(text, prefix=""):
    # Generate a safe string key from text
    key = text.lower()
    key = re.sub(r'[^a-z0-9]+', '_', key)
    key = key.strip('_')
    key = key[:30] # Limit length
    if prefix:
        return f"{prefix}_{key}"
    return key

extracted_strings = {}

def process_file(filepath, file_type):
    if not os.path.exists(filepath):
        return

    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    prefix = os.path.basename(filepath).replace("Screen.kt", "").replace("Components.kt", "").lower()
    
    # Needs import androidx.compose.ui.res.stringResource
    # Needs import com.bioguard.movil.R
    
    # Replace manually one by one with confirmation loop in script or just automated replacements
    # Since this is automated, we'll do a pass over broad texts
    
    # Find all broad texts that aren't inside Log.d etc.
    lines = content.split('\n')
    new_lines = []
    
    for line in lines:
        if 'Log.' in line or 'Exception' in line or 'route = ' in line:
            new_lines.append(line)
            continue
            
        new_line = line
        matches = broad_text_pattern.findall(line)
        for match in matches:
            if len(match) < 2: continue # skip very short strings
            if match.isupper() and len(match) < 4: continue # skip acronyms or icons
            if '{' in match and '}' in match:
                # Handle interpolation manually or skip for now to avoid breaking code
                pass
            else:
                key = generate_key(match, prefix)
                extracted_strings[key] = match
                # Replace in line
                new_line = new_line.replace(f'"{match}"', f'stringResource(R.string.{key})')
                
        new_lines.append(new_line)
        
    new_content = '\n'.join(new_lines)
    
    # Add imports if changed
    if new_content != content:
        if 'androidx.compose.ui.res.stringResource' not in new_content:
            new_content = new_content.replace('import androidx.compose.runtime.Composable', 'import androidx.compose.runtime.Composable\nimport androidx.compose.ui.res.stringResource\nimport com.bioguard.movil.R')
            
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)

for screen in screens:
    process_file(os.path.join(SCREENS_DIR, screen), "screen")
    
for comp in components:
    process_file(os.path.join(COMPONENTS_DIR, comp), "component")

# Write to strings.xml
xml_content = '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <string name="app_name">BioGuard-Movil</string>\n'
for key, value in extracted_strings.items():
    # Escape XML characters
    escaped_value = value.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace("'", "\\'").replace('"', '\\"')
    xml_content += f'    <string name="{key}">{escaped_value}</string>\n'
xml_content += '</resources>'

with open(STRINGS_XML, 'w', encoding='utf-8') as f:
    f.write(xml_content)

print(f"Extracted {len(extracted_strings)} strings.")
