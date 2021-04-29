data class Income(var source: String, var value: Double)
data class Expense(var source: String, var value: Double)

fun main() {
    // My Variables
    var programActive = true
    var expense = mutableListOf<Expense>()
    var income = mutableListOf<Income>()

    // Authenticate Database
    // DbCommunication().initializeDb()


    // Program loop
    println("Welcome! Let's take care of Business.")
    while (programActive) {
        var option: Int
        val message = """
            SELECT AN OPTION.
            1) Add Incomes
            2) Add Expenses
            3) Display Summary
            4) Exit Program
            """.trimMargin()
        println(message)
        option = readLine()?.toInt()!!
        while (option !in 1..4) {
            println("Invalid option. Please try again:")
            option = readLine()?.toInt()!!
        }
        when (option) {
            1 -> if (income == emptyList<Income>()) {
                income = ProgramExecution().addIncomes()
            } else {
                val tempIncome: MutableList<Income> = ProgramExecution().addIncomes()
                income.let { list -> tempIncome.let(list::addAll) }
            }
            2 -> if (expense == emptyList<Expense>()) {
                expense = ProgramExecution().addExpenses()
            } else {
                val tempExpense: MutableList<Expense> = ProgramExecution().addExpenses()
                expense.let { list -> tempExpense.let(list::addAll) }
            }
            3 -> ProgramExecution().displayFinalBalance(income, expense)
            4 -> programActive = false
        }
    }

}

