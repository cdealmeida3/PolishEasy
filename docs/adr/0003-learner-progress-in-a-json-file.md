# Learner progress is saved in one JSON file, not a database

The v1 spec calls for an on-device database for Learner progress. We store it in a single JSON file in the app's private files directory instead, behind the `LearnerProgress` interface. The data is tiny (at most a few hundred small records), the course engine already uses kotlinx.serialization, and a file store lives in the engine module where it is tested on the JVM with no device. A database such as Room would add code generation, a schema and tests that need an emulator, for no benefit at this size.

## Consequences

- Every change rewrites the whole file, through a temporary file and an atomic move, so a crash mid-write leaves the previous version intact.
- A file that cannot be read is moved aside to `progress.json.corrupt` and the Learner starts with empty progress, rather than the app failing to open.
- The file refers to content only by stable ids and ignores fields it does not know, so later features can add to it.
- Item progress (box, next-due date and recently shown Exercises) is stored in the same file, keyed by Item id with no reference to a Level, and written in the same file write as the answer that changed it.
- Review-only Daily Sets are stored as the calendar days they were completed, apart from the completed Daily Sets, which stay a record of the Path. One in progress is saved under the reserved id `review-only`.
- Android's device backup includes the file with no extra work.
- Settings are not in this file. They are in SharedPreferences, which Android's backup also carries; the backup rules name both.
- If progress outgrows a file (many records, or queries), moving to a database means writing one new `LearnerProgress` implementation and a one-time import.
