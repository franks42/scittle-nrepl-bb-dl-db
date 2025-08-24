# Focused UI Design: Main + Reference Statement Panels

## Core UI Requirements

1. **One main statement editor panel** - primary focus for editing
2. **At least one reference panel** - read-only by default for context
3. **All panels read-only by default** - explicit action to make editable
4. **Clear FQN display** - full qualified names visible in title bars
5. **Statement discovery methods**:
   - Browse/search through namespaces
   - Click symbols to find implementations  
   - Ask AI to locate statements
6. **Overlapping panels** - FQNs visible even with multiple panels

## UI Layout Design

### Main Layout: Overlapping Statement Panels
```
┌─────────────────────────────────────────────────────────────┐
│ [🏠] [🔍 Find] [🤖 AI] [📊 Runtime] [💾 Database]          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─ my.app.core/transform-user-data ──────────── ✏️ MAIN ─┐ │
│  │ (defn transform-user-data [users options]              │ │
│  │   (let [validated (filter validate-user users)        │ │  
│  │         processed (map process-user validated)]        │ │
│  │     (if (:sort options)                                │ │
│  │       (sort-by :name processed)                        │ │
│  │       processed)))                                     │ │
│  │                                                        │ │
│  │ [💾 Save] [✅ Validate] [🔒 Make Read-only] [❌ Close] │ │
│  └────────────────────────────────────────────────────────┘ │
│    ┌─ my.app.core/validate-user ──────────── 📖 REF ─┐     │
│    │ (defn validate-user [user]                       │     │
│    │   (and (map? user)                               │     │
│    │        (:email user)                             │     │
│    │        (:name user)                              │     │
│    │        (valid-email? (:email user))))            │     │
│    │                                                  │     │
│    │ [✏️ Edit] [🔗 Dependencies] [❌ Close]           │     │
│    └──────────────────────────────────────────────────┘     │
│      ┌─ my.app.core/process-user ────── 📖 REF ─┐           │
│      │ (defn process-user [user]                │           │
│      │   (-> user                               │           │
│      │       (assoc :processed-at (now))        │           │
│      │       (update :name capitalize)))        │           │
│      │                                          │           │
│      │ [✏️ Edit] [🔗 Find Usage] [❌ Close]     │           │
│      └──────────────────────────────────────────┘           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Panel States and Visual Indicators

#### Main Panel (Editable)
```
┌─ my.app.core/transform-user-data ──────────── ✏️ MAIN ─┐
│ ┌─ [Status Indicators] ─────────────────────────────┐  │
│ │ ✅ Syntax Valid  🟡 Pending Upload  🔗 3 Refs    │  │  
│ └───────────────────────────────────────────────────┘  │
│                                                       │
│ Line numbers │ Code editor with syntax highlighting    │
│ Error marks  │ Autocomplete with FQN suggestions      │
│ AI hints     │ Real-time validation                   │
│                                                       │
│ [💾 Save] [✅ Validate] [🔒 Make Read-only] [❌ Close] │
└───────────────────────────────────────────────────────┘
```

#### Reference Panel (Read-only)
```
┌─ my.app.core/validate-user ──────────── 📖 REF ─┐
│ 📍 Referenced in main panel line 2               │
│                                                  │
│ (defn validate-user [user]                       │
│   (and (map? user)                               │
│        (:email user) ← 🤖 "Could add validation" │
│        (:name user)                              │
│        (valid-email? (:email user))))            │
│                                                  │
│ [✏️ Edit] [🔗 Dependencies] [📋 Copy] [❌ Close] │
└──────────────────────────────────────────────────┘
```

## Statement Discovery Methods

### 1. Namespace Browser (Sidebar - Collapsible)
```
┌─ Browse ────┐
│ 🔍 [Search] │
│             │
│ 📁 Runtime  │
│ ├─🔒my.app  │
│ │ ├─core (12)│ ← Click to expand
│ │ ├─utils(5) │
│ │ └─db (8)   │
│ └─🔒other.ns │
│             │
│ 📝 Database │ 
│ ├─📝my.new  │
│ │ ├─core (3) │ ← Click shows statements
│ │ └─exp (1)  │
│ └─[+ New NS]│
└─────────────┘

// When expanded:
📝 my.new.core (3)
├─ helper [fn] ✅
├─ process [fn] 🟡  
└─ validate [fn] ❌
```

### 2. Symbol Click Navigation
```javascript
// Click any symbol in code to find implementation
onClick: symbol => {
  if (isDefinedSymbol(symbol)) {
    openReferencePanel(symbol);
    highlightDefinition();
  } else {
    showAISuggestion("Create " + symbol + "?");
  }
}
```

### 3. AI-Assisted Discovery
```
┌─ AI Assistant ─────────────────────────────────────┐
│ > Find the function that validates email addresses │
│                                                 [⏎]│
│                                                    │
│ 🤖 I found: my.app.core/valid-email?              │
│    [📖 Show as Reference] [✏️ Open for Editing]   │
│                                                    │
│ > Show me all functions that use validate-user    │
│                                                 [⏎]│
│                                                    │
│ 🤖 Found 3 functions:                             │
│    • my.app.core/transform-user-data              │
│    • my.app.core/bulk-process                     │  
│    • my.app.api/create-user                       │
│    [📖 Show All] [🎯 Focus on One]                │
└────────────────────────────────────────────────────┘
```

## Panel Management

### Overlapping Panel System
```
Stack Order (z-index):
┌─ Main Panel ──────────────────────── z:100 ─┐
│                                             │
│  ┌─ Ref Panel 1 ─────────────── z:90 ─┐    │
│  │                                     │    │
│  │  ┌─ Ref Panel 2 ────────── z:80 ─┐ │    │
│  │  │                               │ │    │
│  │  │  Only title bars visible      │ │    │
│  │  └───────────────────────────────┘ │    │
│  │                                     │    │
│  └─────────────────────────────────────┘    │
│                                             │
└─────────────────────────────────────────────┘

Visual: User sees all FQNs even when panels overlap
```

### Panel Controls
```
Panel Title Bar Actions:
┌─ my.very.long.namespace/function-name ─── ✏️ MAIN ─┐
│ [📍 Pin] [📖→✏️ Edit] [🔗 Refs] [📋 Copy] [❌ Close] │
│                                                     │

Pin: Keep panel visible when opening others
Edit: Switch from read-only to editable (only one editable at a time)
Refs: Show dependency/usage information
Copy: Copy FQN to clipboard
Close: Remove panel
```

## Interaction Patterns

### Opening Statements
```javascript
// 1. From namespace browser
openStatement(fqn, mode = 'reference') {
  createPanel(fqn, mode);
  if (mode === 'main') {
    setOthersToReadOnly();
  }
}

// 2. From symbol click
onSymbolClick(symbol, currentPanel) {
  const fqn = resolveSymbol(symbol);
  openStatement(fqn, 'reference');
  highlightInPanel(currentPanel, symbol);
}

// 3. From AI suggestion
aiOpenStatement(fqn, reason) {
  openStatement(fqn, 'reference');
  showAIMessage(`Opening ${fqn}: ${reason}`);
}
```

### Switching Main/Reference
```javascript
// Only one main panel at a time
switchToMain(panelId) {
  currentMain?.switchToReference();
  panels[panelId].switchToMain();
  enableEditing(panelId);
}
```

### AI Integration with Panel Management
```javascript
// AI can control panel layout
aiActions = {
  showContext(mainFqn, contextFqns) {
    openStatement(mainFqn, 'main');
    contextFqns.forEach(fqn => 
      openStatement(fqn, 'reference')
    );
  },
  
  focusError(fqn, lineNumber) {
    openStatement(fqn, 'main');
    highlightLine(lineNumber);
    showAIExplanation();
  }
};
```

## Visual Design Details

### Panel Styling
```css
/* Main panel - prominent border, edit controls */
.panel-main {
  border: 3px solid #007acc;
  box-shadow: 0 4px 12px rgba(0,122,204,0.3);
  z-index: 100;
}

/* Reference panels - subtle, stackable */
.panel-reference {
  border: 1px solid #ccc;
  opacity: 0.9;
  z-index: calc(90 - var(--stack-order));
}

/* FQN title bars always visible */
.panel-title {
  background: linear-gradient(to right, #f8f8f8, #e8e8e8);
  padding: 4px 8px;
  font-family: monospace;
  font-size: 12px;
  font-weight: bold;
  min-height: 24px;
}
```

### Responsive Behavior
```javascript
// Auto-arrange panels based on screen size
function arrangePanels() {
  const screenWidth = window.innerWidth;
  
  if (screenWidth > 1400) {
    // Large screen: side-by-side layout
    arrangeHorizontally();
  } else if (screenWidth > 1000) {
    // Medium: overlapping with more offset
    arrangeOverlapping(40);
  } else {
    // Small: minimal overlap, focus on main
    arrangeOverlapping(20);
  }
}
```

This design prioritizes clarity and focus while providing the context and reference capabilities needed for understanding code relationships and dependencies.