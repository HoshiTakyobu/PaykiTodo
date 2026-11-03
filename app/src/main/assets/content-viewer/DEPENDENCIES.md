# Offline reader dependencies

- marked 18.1.3, MIT, `vendor/marked.js`, upstream https://github.com/markedjs/marked
- DOMPurify 3.4.17, Apache-2.0 OR MPL-2.0, `vendor/purify.js`, upstream https://github.com/cure53/DOMPurify

The exact npm release distributions and their full licenses are bundled in `vendor/`. Both clients package these same files. No CDN resources are requested. Marked supplies GFM tables, tasks and nested lists; DOMPurify plus the hosts' resource/navigation allowlists enforce the display boundary. Raw HTML is escaped and images are presented as text.
