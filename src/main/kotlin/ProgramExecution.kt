import java.math.BigDecimal

class ProgramExecution {

    fun addIncomes(): MutableList<Income> {
        val incomes = mutableListOf<Income>()
        println("ADD INCOMES")
        var status = ""
        while (status != "q") {
            println("Income Source: ")
            val source = readLine().toString()

            println("Income Value: ")
            var value = readLine()?.toDoubleOrNull()
            while (value == null || getNumberOfDecimalPlaces(value.toBigDecimal()) > 2 || value < 0) {
                println("Invalid value. Please try again:")
                value = readLine()?.toDoubleOrNull()
            }
            incomes.add(Income(source, value))

            println("To add another income, press ENTER. To finish, press 'q' then ENTER")
            status = readLine().toString().toLowerCase()
        }
        return incomes
    }

    private fun displayIncome(myIncomes: MutableList<Income>): Double {
        var incomesTotal = 0.0
        println("YOUR INCOMES:")
        for (i in myIncomes.indices) {
            incomesTotal += myIncomes[i].value
            println("${myIncomes[i].source} - $${myIncomes[i].value}")
        }
        println("INCOMES TOTAL: $${incomesTotal}")
        println("\n")
        return incomesTotal
    }

    fun addExpenses(): MutableList<Expense> {
        val expenses = mutableListOf<Expense>()
        println("ADD EXPENSES")
        var status = ""
        while (status != "q") {
            println("Expense Source: ")
            val source = readLine().toString()

            println("Expense Value: ")
            var value = readLine()?.toDoubleOrNull()
            while (value == null || getNumberOfDecimalPlaces(value.toBigDecimal()) > 2 || value < 0.0) {
                println("Invalid value. Please try again:")
                value = readLine()?.toDoubleOrNull()
            }
            expenses.add(Expense(source, value))

            println("To add another expense, press ENTER. To finish, press 'q' then ENTER")
            status = readLine().toString().toLowerCase()
        }
        return expenses
    }

    private fun displayExpense(myExpenses: MutableList<Expense>): Double {
        println("YOUR EXPENSES:")
        var expensesTotal = 0.0
        for (i in myExpenses.indices) {
            expensesTotal += myExpenses[i].value
            println("${myExpenses[i].source} - $${myExpenses[i].value}")
        }
        println("EXPENSES TOTAL: $${expensesTotal}")
        return expensesTotal
    }

    fun displayFinalBalance(income: MutableList<Income>, expense: MutableList<Expense>) {
        val incomeTotal: Double = displayIncome(income)
        val expenseTotal: Double = displayExpense(expense)
        println("--------------------------------------------")
        println("FINAL BALANCE: $${incomeTotal.toBigDecimal() - expenseTotal.toBigDecimal()}")
        println("--------------------------------------------")
    }

    private fun getNumberOfDecimalPlaces(number: BigDecimal?): Int {
        val scale = number?.stripTrailingZeros()?.scale()
        return if (scale != null) {
            if (scale > 0) scale else 0
        } else
            0
    }
}