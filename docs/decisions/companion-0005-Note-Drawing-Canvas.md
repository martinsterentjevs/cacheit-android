# Companion for ADR 0005 
Created:01/09/2026

---

> This is a companion document for the ADR 0005 for NoteEdit redesign canvas outlining the different
> ideas considered for the logical space and why that idea was made
---

## The goal

Reach a note text and drawing relationship where a note drawing can link up to the text in a persistently rendered way

## Timeline of ideas

- Canvas is a logical space 1000*1000 logical units wide (25/08-29/08) - The original idea meant to 
outline a way for a note text and drawing to match up. Scrapped for a bigger surface as the limiter was believed to be the better option.
-  Canvas increased to 4000*4000 logical units (29/08-01/09) - a sized up canvas for a rendering where a note logical space is above a 4K screen limit therefore making rendering easier. Scrapped due to zoom interactions with viewport not making sense
- Canvas is a more-paper based ratio of a fixed horizontal width and infinite scroll length (01/09-) - Idea inspired to avoid the zoom-out interaction not clamping to a note text borders and pulling toward a imaginary square of the viewport. 
- - additionally the Layered approach is getting changed to a unified canvas with both text and drawing following a json structure. Originally the two separate layers were though of being the easier way for drawing and text interaction, but implementing it in any of these methods without making then actually be based off of the same coordinate rules. Text  json would outline the lineheight - taken from the TypeBody type, have some content and include any of the markdown tooling added in.
    the unified approach leads to points like this 
  
  | Mode            | Text              | Drawing         |
  |-----------------|-------------------|-----------------|
  | **Create**      | Markdown editing  | Drawing editing |
  | **View**        | Rendered Markdown | Rendered        |
  | **TextEdit**    | Markdown editing  | Rendered        |
  | **DrawingEdit** | Rendered Markdown | Drawing editing |

NoteDocument structure (NoteDto dto element encBody):
schema_version= 1,
elements ={
    noteElement{
        startLine = 1,
        content = "Lorem ipsum dolor ~~sat~~ sit amet",
        },
}


So the semantics are:

Each newline establishes the next logical line. Each NoteElement occupies a logical line beginning at startLine. Markdown rendering determines the appearance within that line's available space, while the document model preserves the line's spatial position.