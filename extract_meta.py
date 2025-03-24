import os, re

# Ajusta aquí la ruta raw hacia tu carpeta java principal
root = r"C:\Users\Ssanc\AndroidStudioProjects\edukrd\app\src\main\java"

data_classes, viewmodels, screens = [], [], []

for dirpath, _, files in os.walk(root):
    for f in files:
        if f.endswith(".kt"):
            path = os.path.join(dirpath, f)
            text = open(path, encoding="utf-8", errors="ignore").read()
            if "data class " in text:
                data_classes.append(f)
            if re.search(r"class\s+\w+ViewModel", text):
                viewmodels.append(f)
            if "@Composable" in text:
                screens.append(f)

print("DATA_CLASSES:", data_classes)
print("VIEWMODELS:", viewmodels)
print("SCREENS:", screens)
