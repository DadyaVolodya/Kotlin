package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import mobi.sevenwinds.app.author.AuthorCreateRequest
import mobi.sevenwinds.app.author.AuthorRecord
import mobi.sevenwinds.app.author.AuthorTable
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction {
            BudgetTable.deleteAll()
            AuthorTable.deleteAll()
        }
    }

    @Test
    fun testBudgetPagination() {
        addRecord(BudgetRecord(2020, 5, 10, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 20, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 40, BudgetType.Приход))
        addRecord(BudgetRecord(2030, 1, 1, BudgetType.Расход))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assert.assertEquals(5, response.total)
                Assert.assertEquals(3, response.items.size)
                Assert.assertEquals(105, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetRecord(2020, 5, 100, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 400, BudgetType.Приход))

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                // Check month ascending order
                Assert.assertEquals(1, response.items[0].month)
                Assert.assertEquals(1, response.items[1].month)
                Assert.assertEquals(5, response.items[2].month)
                Assert.assertEquals(5, response.items[3].month)
                Assert.assertEquals(5, response.items[4].month)

                // Check amount descending order within same month
                Assert.assertEquals(30, response.items[0].amount)
                Assert.assertEquals(5, response.items[1].amount)
                Assert.assertEquals(400, response.items[2].amount)
                Assert.assertEquals(100, response.items[3].amount)
                Assert.assertEquals(50, response.items[4].amount)
            }
    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetRecord(2020, -5, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetRecord(2020, 15, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)
    }

    @Test
    fun testBudgetWithAuthor() {
        // Create author
        val author = RestAssured.given()
            .jsonBody(AuthorCreateRequest("John Doe"))
            .post("/author/add")
            .toResponse<AuthorRecord>()

        // Create budget record with author
        val record = BudgetRecord(2020, 5, 100, BudgetType.Приход, author.id)
        val savedRecord = addRecord(record)

        // Verify author is included in response
        Assert.assertEquals(author.id, savedRecord.authorId)
        Assert.assertEquals(author.fullName, savedRecord.author?.fullName)

        // Test author name filter
        RestAssured.given()
            .queryParam("authorNameFilter", "john")
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                Assert.assertEquals(1, response.items.size)
                Assert.assertEquals(author.id, response.items[0].authorId)
            }

        // Test case-insensitive author name filter
        RestAssured.given()
            .queryParam("authorNameFilter", "JOHN")
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                Assert.assertEquals(1, response.items.size)
                Assert.assertEquals(author.id, response.items[0].authorId)
            }
    }

    private fun addRecord(record: BudgetRecord): BudgetRecord {
        return RestAssured.given()
            .jsonBody(record)
            .post("/budget/add")
            .toResponse()
    }
} 