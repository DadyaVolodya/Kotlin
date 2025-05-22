# Test Task

## Tasks

1. Fix tests
* `testBudgetPagination` - fix pagination and total statistics calculation
* `testStatsSortOrder` - implement correct sorting order

2. Remove `Комиссия` from `BudgetType` and replace with `Расход` via DB migration

3. Add `Author` table
* Columns: `ID`, `ФИО`, `Дата создания` (timestamp)
* Add API endpoint for creating new Author records
* Add optional `Author.id` reference to `BudgetTable`
* Update `/budget/add` to accept optional author ID
* Include author name and creation timestamp in `/budget/year/{year}/stats` response
* Add case-insensitive author name filter to `/budget/year/{year}/stats`

## Project Structure

```
src/
├── main/
│   ├── kotlin/mobi/sevenwinds/app/
│   │   ├── author/
│   │   │   ├── AuthorApi.kt
│   │   │   ├── AuthorService.kt
│   │   │   └── AuthorTable.kt
│   │   └── budget/
│   │       ├── BudgetApi.kt
│   │       ├── BudgetService.kt
│   │       ├── BudgetTable.kt
│   │       └── BudgetType.kt
│   └── resources/db/migration/
│       ├── V2__remove_commission_type.sql
│       └── V3__create_author_table.sql
└── test/
    └── kotlin/mobi/sevenwinds/app/budget/
        └── BudgetApiKtTest.kt
```
