# 0003-Note-History-Marking
Created: 25/08/2026
Status: Accepted 

## Context
`NoteCard` component holds two indications that server as a marker for showing the Notecard has more versions available:
the cut bottom right corner and a history symbol. There are three paths to choose from based on the `Scope_of_PreMVP-Checklist.md` document - choose to leave the icon only, the cut corner only or a directly unmentioned leave both option.

## Decision
`NoteCard` can remain to have both markers for a few reasons. Firstly a history icon serves as a fine button to access the `NoteVersionScreen` - saves on a separate line item in a different context menu.
Secondly, the cut corner marker can still act as a differentiator when scrolling through notes and it does not take away from a user's experience. And thirdly, leaving both reduces work for already live components. 

## Alternatives considered
- Icon only - Keeps the access to `NoteVersionHistory` but loses a secondary characteristic, makes only difference an icon making a realization of history being.
- Corner only - Changes how to access `NoteVersionHistory` to be in a context menu. More work for not enough of a realization that the note has a history that a user might miss on their own