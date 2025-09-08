# Rendering Mermaid Diagrams

## Option 1: VS Code/Cursor Extension
1. Install "Markdown Preview Mermaid Support" extension
2. Install "Mermaid Markdown Syntax Highlighting"

## Option 2: Command Line (Node.js)
```bash
# Install mermaid-cli locally in the project
npm install @mermaid-js/mermaid-cli

# Render diagrams
npx mmdc -i detailed-flow.mmd -o detailed-flow.svg
npx mmdc -i data-flow.mmd -o data-flow.svg
npx mmdc -i folder-structure.mmd -o folder-structure.svg
```

## Option 3: Online Editor
1. Visit [Mermaid Live Editor](https://mermaid.live)
2. Paste the .mmd content
3. Export as SVG/PNG

## Quick Render Script
```bash
#!/bin/bash
# Save as render.sh

for file in *.mmd; do
  npx mmdc -i "$file" -o "${file%.mmd}.svg"
  npx mmdc -i "$file" -o "${file%.mmd}.png"
done
```
