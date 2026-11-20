# TaskFlow verification cases

From the project root run `taskflow.cmd test` and `taskflow.cmd ui-test` in PowerShell using the .\ prefix. In Git Bash or on Linux/macOS use `bash taskflow.sh test` and `bash taskflow.sh ui-test`. Both suites use new temporary directories. The desktop suite opens real windows and closes them when finished. Pass an image directory after ui-test to capture the operations.

## Desktop workflow

| ID | Scenario | Input or action | Expected | Observed | Result |
|---|---|---|---|---|---|
| D01 | View an empty list | Open a new workspace. | Empty message and disabled Edit. | No tasks yet. Edit disabled. Rows = 0 | PASS |
| D02 | Reject an empty title | Open Add task and submit without typing. | Title prompt, zero tasks, form stays open. | Enter a task title. | PASS |
| D03 | Add a valid task | Add Prepare the OEL report as Pending. | One row with the entered title and stage. | Prepare the OEL report / Pending | PASS |
| D04 | View all tasks | Add Review class diagram and Generate Javadoc. | All three titles appear in insertion order. | All 3 titles in creation order. | PASS |
| D05 | Update an existing task | Rename the first row to Print the OEL report, change its notes and choose In Progress. | Revised title, notes and stage with the same ID. | Print the OEL report / In Progress. Same ID. | PASS |
| D06 | Change and filter status | Move that row to Completed. Select Completed. | Exactly two completed rows are shown. | Completed filter shows 2 completed tasks. | PASS |
| D07 | Combine search and status | In Completed, search for “  API INDEX  ” in the notes. | One match, ignoring case and outer spaces. | Notes search + status filter returned Generate Javadoc. | PASS |
| D08 | View no matches | Search for “no such phrase”. | No matching tasks and zero visible rows. | No matching tasks. Rows = 0 | PASS |
| D09 | Cancel a deletion | Select Review class diagram, click Delete and accept the default Cancel. | All three rows remain. | Cancel retained the selected task. Rows = 3 | PASS |
| D10 | Delete an existing task | Reopen Delete for the same row and click Delete task. | The selected row disappears. Two remain. | Review class diagram removed. Rows = 2 | PASS |
| D11 | Reopen the application | Close and relaunch against the same workspace. | Two saved tasks and the same edited ID and status. | Reopened with 2 tasks. Edited ID and status retained. | PASS |
| D12 | Resize the window | Resize the window to its 790 × 500 minimum. | Add, search and the task area remain usable. | Add, search and task rows visible at minimum size. | PASS |

## Model and local storage

| ID | Scenario | Input or action | Expected | Observed | Result |
|---|---|---|---|---|---|
| M01 | Update a missing ID | Update a fresh UUID in an empty workspace. | Missing-task error. List stays empty. | This task no longer exists. Count = 0 | PASS |
| M02 | Delete a missing ID | Delete a fresh UUID in an empty workspace. | Missing-task error. List stays empty. | This task no longer exists. Count = 0 | PASS |
| M03 | Distinguish duplicate titles | Add “  Notes  ” and “Notes”, then delete the second by ID. | Only the first ID remains. Its title is trimmed. | Duplicate removed by ID. Remaining title = Notes | PASS |
| M04 | Reject a blank edit | Replace a valid title with spaces and a tab. | Validation error. Original record remains. | Enter a task title. Original task retained. | PASS |
| M05 | Reject an absent status | Pass null as the stage for a new task. | Status error. Existing count stays one. | Choose a task status. Count = 1 | PASS |
| M06 | Prevent a second writer | Open the same directory twice before closing the first store. | Second open reports that the workspace is in use. | This workspace is already open. | PASS |
| M07 | Round-trip text and identity | Save Urdu text, a newline, equals, colon and backslash. Close and reopen. | ID, stage and description match exactly. | Same ID, status and UTF-8 multiline text after reopen. | PASS |
| M08 | Handle a write failure | Block the data-file path with a non-empty directory, then add a task. | Save fails. The list stays empty. | Save refused. In-memory count = 0 | PASS |
| M09 | Retry the failed write | Remove the test obstruction and add Retry. | One task in memory and on disk. | Retry saved 1 task. | PASS |
| M10 | Preserve a corrupt file | Open an incomplete properties file with an invalid record. | Load fails. Original bytes remain intact. | Invalid file reported. Original bytes retained. | PASS |

For M01 and M02, the test supplies a fresh UUID to TaskManager. The normal table only permits actions on selected, existing rows. M08 and M10 arrange failures in their temporary directories.
