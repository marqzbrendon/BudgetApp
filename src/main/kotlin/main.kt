import com.google.api.core.ApiFuture
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.*
import java.io.FileInputStream
import java.math.BigDecimal
import java.time.Year

// data classes to hold data
data class Data(
    var month: Long = 0L,
    var year: Long = 0L,
    var incomesDb: MutableList<Input> = mutableListOf(),
    var expensesDb: MutableList<Input> = mutableListOf(),
    var incomesDbKeys: MutableList<String> = mutableListOf(),
    var expensesDbKeys: MutableList<String> = mutableListOf()
)

data class Input(
    val source: String,
    val value: Double
)

//***********************************************************************
//************************** PROGRAM MAIN LOOP **************************
//***********************************************************************
fun main() {
    // Welcome message
    println("Welcome! Let's take care of Business.")

    // Variable to keep the program running
    var programActive = true

    while (programActive) {
        // Variable to keep the current month/year active
        var monthYearActive = true

        // Program loop
        val data = Data()

        // Get Period
        var validPeriod: Boolean = getPeriod(data)
        while (!validPeriod) {
            validPeriod = getPeriod(data)
        }

        // Initialize Database and get current data
        val db: Firestore = initializeDb(data)

        while (monthYearActive) {
            var option: Int?
            val message =
                """SELECT AN OPTION.
            1) Incomes
            2) Expenses
            3) Display Summary
            7) Change Month/Year
            8) Delete All Data
            9) Exit Program
            """.trimMargin()
            println(message)
            option = readLine()?.toIntOrNull()
            while (option !in 1..3 && option !in 7..9 || option == null) {
                println("Invalid option. Please try again:")
                option = readLine()?.toIntOrNull()
            }
            when (option) {
                1 -> incomes(db, data)
                2 -> expenses(db, data)
                3 -> displayFinalBalance(data)
                7 -> monthYearActive = false
                8 -> deleteAll(db, data)
                9 -> {
                    programActive = false
                    monthYearActive = false
                }
            }
        }
    }

}

//***********************************************************************
//*********************** PROGRAM BASIC FUNCTIONS ***********************
//***********************************************************************
fun getPeriod(data: Data): Boolean {
    println("Select Period (MM-YYYY): ")
    val periodRaw = readLine()
    if (!checkLength(periodRaw)) {
        println("Invalid Period. Please try again.")
        return false
    }
    if (!checkDash(periodRaw)) {
        println("Invalid Period. Please try again.")
        return false
    }
    if (!checkIndividualLength(periodRaw)) {
        println("Invalid Period. Please try again.")
        return false
    }
    if (!checkIfNumbers(periodRaw)) {
        println("Invalid Period. Please try again.")
        return false
    }
    if (!checkIfValidNumbers(periodRaw)) {
        println("Invalid Period. Please try again.")
        return false
    }

    if (periodRaw != null) {
        val period = periodRaw.split("-")
        data.month = period[0].toLong()
        data.year = period[1].toLong()
    }
    return true
}

fun checkIfValidNumbers(periodRaw: String?): Boolean {
    val period = periodRaw!!.split("-")
    return period[0].toLong() in 1..12 && period[1].toLong() <= Year.now().value
}

fun checkIfNumbers(periodRaw: String?): Boolean {
    val period = periodRaw!!.split("-")
    return period[0].toDoubleOrNull() != null && period[1].toDoubleOrNull() != null
}

fun checkIndividualLength(periodRaw: String?): Boolean {
    val period = periodRaw!!.split("-")
    return period[0].length == 2 && period[1].length == 4
}

fun checkDash(periodRaw: String?): Boolean {
    return periodRaw?.contains("-")!!
}

fun checkLength(periodRaw: String?): Boolean {
    return periodRaw?.length == 7
}

fun incomes(db: Firestore, data: Data) {
    var incomeScreen = true
    while (incomeScreen) {
        var option: Int?
        val message = """
            INCOMES MENU.
            1) Add an Income
            2) Edit an Income
            3) Delete an Income
            9) Return to Main Menu
            """.trimMargin()
        println(message)
        option = readLine()?.toIntOrNull()
        while (option !in 1..3 && option != 9 || option == null) {
            println("Invalid option. Please try again:")
            option = readLine()?.toIntOrNull()
        }
        when (option) {
            1 -> addIncomes(db, data)
            2 -> editIncome(db, data)
            3 -> deleteIncome(db, data)
            9 -> incomeScreen = false
        }
    }
}

fun expenses(db: Firestore, data: Data) {
    var expenseScreen = true
    while (expenseScreen) {
        var option: Int?
        val message = """
            EXPENSES MENU.
            1) Add an Expense
            2) Edit an Expense
            3) Delete an Expense
            9) Return to Main Menu
            """.trimMargin()
        println(message)
        option = readLine()?.toIntOrNull()
        while (option !in 1..3 && option != 9 || option == null) {
            println("Invalid option. Please try again:")
            option = readLine()?.toIntOrNull()
        }
        when (option) {
            1 -> addExpenses(db, data)
            2 -> editExpense(db, data)
            3 -> deleteExpense(db, data)
            9 -> expenseScreen = false
        }
    }
}

fun addIncomes(db: Firestore, data: Data) {
    // Get the new income source and value
    println("ADD INCOMES")
    var status = ""
    while (status != "n") {
        println("Income Source (to cancel, type 0): ")
        val source = readLine().toString()
        if (source == "0") {
            return
        }

        println("Income Value (to cancel, type 0): ")
        var value = readLine()?.toDoubleOrNull()
        if (value == 0.0) {
            return
        }
        while (value == null || getNumberOfDecimalPlaces(value.toBigDecimal()) > 2 || value < 0) {
            println("Invalid value. Please try again:")
            value = readLine()?.toDoubleOrNull()
            if (value == 0.0) {
                return
            }
        }

        // Call the database to add the new income
        addIncomeDb(db, Input(source, value), data)

        // Ask if user would like to add another income
        println("Would you like to add another income? y/n")
        status = readLine()!!.toLowerCase()
        while (status != "n" && status != "y") {
            println("Invalid input. Would you like to add another income? y/n")
            status = readLine()!!.toLowerCase()
        }
    }
}

fun addExpenses(db: Firestore, data: Data) {
    // Get the new expense source and value
    println("ADD EXPENSES")
    var status = ""
    while (status != "n") {
        println("Expense Source (to cancel, type 0: ")
        val source = readLine().toString()
        if (source == "0") {
            return
        }

        println("Expense Value (to cancel, type 0): ")
        var value = readLine()?.toDoubleOrNull()
        if (value == 0.0) {
            return
        }
        while (value == null || getNumberOfDecimalPlaces(value.toBigDecimal()) > 2 || value < 0.0) {
            println("Invalid value. Please try again:")
            value = readLine()?.toDoubleOrNull()
            if (value == 0.0) {
                return
            }
        }

        // Call the database to add the new expense
        addExpenseDb(db, Input(source, value), data)

        // Ask if user would like to add another expense
        println("Would you like to add another expense? y/n")
        status = readLine()!!.toLowerCase()
        while (status != "n" && status != "y") {
            println("Invalid input. Would you like to add another expense? y/n")
            status = readLine()!!.toLowerCase()
        }
    }
}

// Display the list of incomes and their total
fun displayIncome(data: Data, displayCancel: Boolean): Double {
    var incomesTotal = 0.0
    println("YOUR INCOMES:")
    for (i in data.incomesDb.indices) {
        incomesTotal += data.incomesDb[i].value
        println("${i + 1}. ${data.incomesDb[i].source} - $${data.incomesDb[i].value}")
    }
    if (displayCancel) {
        println("\n0. Cancel")
        return 0.0
    }
    println("INCOMES TOTAL: $${incomesTotal}")
    println("\n")
    return incomesTotal
}

// Display the list of expenses and their total
fun displayExpense(data: Data, displayCancel: Boolean): Double {
    println("YOUR EXPENSES:")
    var expensesTotal = 0.0
    for (i in data.expensesDb.indices) {
        expensesTotal += data.expensesDb[i].value
        println("${i + 1}. ${data.expensesDb[i].source} - $${data.expensesDb[i].value}")
    }
    if (displayCancel) {
        println("\n0. Cancel")
        return 0.0
    }
    println("EXPENSES TOTAL: $${expensesTotal}")
    return expensesTotal
}

// Display both the income total and the expanse total, and subtract their values
fun displayFinalBalance(data: Data) {
    val incomeTotal = displayIncome(data, false)
    val expenseTotal = displayExpense(data, false)

    println("--------------------------------------------")
    println("FINAL BALANCE: $${incomeTotal.toBigDecimal() - expenseTotal.toBigDecimal()}")
    println("--------------------------------------------")
}

// Edit any income
fun editIncome(db: Firestore, data: Data) {
    if (data.incomesDb.size < 1) {
        println("There are no incomes to edit.")
        return
    }

    var status = ""
    while (status != "n") {
        println("Which income would you like to edit? (Select a number from 1 to ${data.incomesDb.size}, or 0 to cancel)")
        displayIncome(data, true)
        var index: Int = readLine()?.toIntOrNull()!!
        if (index == 0) {
            return
        }
        while (index !in 1..data.incomesDb.size) {
            println("Invalid option. Please select a number from 1 to ${data.incomesDb.size}, or 0 to cancel)")
            index = readLine()?.toIntOrNull()!!
            if (index == 0) {
                return
            }
        }

        println("Income Source (to cancel, type 0): ")
        val source = readLine().toString()
        if (source == "0") {
            return
        }

        println("Income Value (to cancel, type 0): ")
        var value = readLine()?.toDoubleOrNull()
        if (value == 0.0) {
            return
        }
        while (value == null || getNumberOfDecimalPlaces(value.toBigDecimal()) > 2 || value < 0.0) {
            println("Invalid value. Please try again:")
            value = readLine()?.toDoubleOrNull()
            if (value == 0.0) {
                return
            }
        }

        editIncomeDb(db, data, index, Input(source, value))

        println("Would you like to edit another income? y/n")
        status = readLine()!!.toLowerCase()
        while (status != "n" && status != "y") {
            println("Invalid input. Would you like to add another income? y/n")
            status = readLine()!!.toLowerCase()
        }
    }
}

// Edit any expense
fun editExpense(db: Firestore, data: Data) {
    if (data.expensesDb.size < 1) {
        println("There are no expenses to edit.")
        return
    }

    var status = ""
    while (status != "n") {
        println("Which expense would you like to edit? (Select a number from 1 to ${data.incomesDb.size}, or 0 to cancel)")
        displayExpense(data, true)
        var index: Int = readLine()?.toIntOrNull()!!
        if (index == 0) {
            return
        }
        while (index !in 1..data.expensesDb.size) {
            println("Invalid option. Please select a number from 1 to ${data.expensesDb.size}, or 0 to cancel")
            index = readLine()?.toIntOrNull()!!
            if (index == 0) {
                return
            }
        }

        println("Expense Source (to cancel, enter 0): ")
        val source = readLine().toString()
        if (source == "0") {
            return
        }

        println("Expense Value: ")
        var value = readLine()?.toDoubleOrNull()
        if (value == 0.0) {
            return
        }
        while (value == null || getNumberOfDecimalPlaces(value.toBigDecimal()) > 2 || value < 0.0) {
            println("Invalid value. Please try again:")
            value = readLine()?.toDoubleOrNull()
            if (value == 0.0) {
                return
            }
        }

        editExpenseDb(db, data, index, Input(source, value))

        println("Would you like to edit another expense? y/n")
        status = readLine()!!.toLowerCase()
        while (status != "n" && status != "y") {
            println("Invalid input. Would you like to add another expense? y/n")
            status = readLine()!!.toLowerCase()
        }
    }
}

// Validates the input for income and expanse values
fun getNumberOfDecimalPlaces(number: BigDecimal?): Int {
    val scale = number?.stripTrailingZeros()?.scale()
    return if (scale != null) {
        if (scale > 0) scale else 0
    } else
        0
}

// Delete any income
fun deleteIncome(db: Firestore, data: Data) {
    if (data.incomesDb.size < 1) {
        println("There are no incomes to delete.")
        return
    }
    println("Which income would you like to delete? (Select a number from 1 to ${data.incomesDb.size}, or 0 to cancel)")
    displayIncome(data, true)
    var index: Int = readLine()?.toIntOrNull()!!
    if (index == 0) {
        return
    }
    while (index !in 1..data.incomesDb.size) {
        println("Invalid option. Please select a number from 1 to ${data.incomesDb.size}, or 0 to cancel")
        index = readLine()?.toIntOrNull()!!
        if (index == 0) {
            return
        }
    }
    println("The income \"${data.incomesDb[index - 1].source}\" will be deleted. Do you with to continue? y/n")
    var confirm = readLine()!!.toLowerCase()
    while (confirm != "n" && confirm != "y") {
        println("Invalid input. Would you like to delete \"${data.incomesDb[index - 1].source}\"? y/n")
        confirm = readLine()!!.toLowerCase()
    }
    if (confirm == "y") {
        deleteIncomeDb(db, data, index)
    }
}

// Delete any expense
fun deleteExpense(db: Firestore, data: Data) {

    if (data.expensesDb.size < 1) {
        println("There are no expenses to delete.")
        return
    }
    println("Which expense would you like to delete? (Select a number from 1 to ${data.expensesDb.size}, or 0 to cancel)")
    displayExpense(data, true)
    var index: Int = readLine()?.toIntOrNull()!!
    if (index == 0) {
        return
    }
    while (index !in 1..data.expensesDb.size) {
        println("Invalid option. Please select a number from 1 to ${data.expensesDb.size}, or 0 to cancel")
        index = readLine()?.toIntOrNull()!!
        if (index == 0) {
            return
        }
    }
    println("The expense \"${data.expensesDb[index - 1].source}\" will be deleted. Do you with to continue? y/n")
    var confirm = readLine()!!.toLowerCase()
    while (confirm != "n" && confirm != "y") {
        println("Invalid input. Would you like to delete \"${data.expensesDb[index - 1].source}\"? y/n")
        confirm = readLine()!!.toLowerCase()
    }
    if (confirm == "y") {
        deleteExpenseDb(db, data, index)
    }
}

//***********************************************************************
//******************* COMMUNICATING WITH THE DATABASE *******************
//***********************************************************************

fun initializeDb(data: Data): Firestore {
    // Initialize and authenticate the application
    val serviceAccount = FileInputStream("src/main/kotlin/auth_key.json")
    val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
        .setProjectId("budget-acd6b")
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build()
    // Get the data from the database as the program initiates
    retrieveAllDocuments(firestoreOptions.service, data)
    return firestoreOptions.service
}

// Push an income to the database
fun addIncomeDb(db: Firestore, income: Input, data: Data) {
    val incomeDb = HashMap<String, Any>()
    incomeDb["source"] = income.source
    incomeDb["value"] = income.value
    db.collection("${data.year}").document("${data.month}").collection("income").document().set(incomeDb)
}

// Push an expense to the database
fun addExpenseDb(db: Firestore, expense: Input, data: Data) {
    val expenseDb = HashMap<String, Any>()
    expenseDb["source"] = expense.source
    expenseDb["value"] = expense.value
    db.collection("${data.year}").document("${data.month}").collection("expense").document().set(expenseDb)
}

// Loop through the income and expense collections in the database, and store the data in local variables.
// The 'onEvent' function will listen to any changes on the database, and automatically retrieve the changed data.
fun retrieveAllDocuments(db: Firestore?, data: Data) {
    db?.collection("${data.year}")?.document("${data.month}")?.collection("income")
        ?.addSnapshotListener(object : EventListener<QuerySnapshot?> {
            override fun onEvent(snapshots: QuerySnapshot?, e: FirestoreException?) {
                data.incomesDbKeys.clear()
                data.incomesDb.clear()
                if (e != null) {
                    println("Listen failed:$e")
                    return
                }
                if (snapshots != null) {
                    for (doc in snapshots) {
                        doc.getString("source")?.let {
                            doc.getDouble("value")
                                ?.let { it1 -> Input(it, it1) }
                        }?.let { data.incomesDb.add(it) }
                        data.incomesDbKeys.add(doc.id)
                    }
                }
            }
        })

    db?.collection("${data.year}")?.document("${data.month}")?.collection("expense")
        ?.addSnapshotListener(object : EventListener<QuerySnapshot?> {
            override fun onEvent(snapshots: QuerySnapshot?, e: FirestoreException?) {
                data.expensesDb.clear()
                data.expensesDbKeys.clear()
                if (e != null) {
                    System.err.println("Listen failed:$e")
                    return
                }
                if (snapshots != null) {
                    for (doc in snapshots) {
                        doc.getString("source")?.let {
                            doc.getDouble("value")
                                ?.let { it1 -> Input(it, it1) }
                        }?.let { data.expensesDb.add(it) }
                        data.expensesDbKeys.add(doc.id)
                    }
                }
            }
        })
}

// Edit an income in the database
fun editIncomeDb(db: Firestore, data: Data, index: Int, newData: Input) {
    val docRef = db.collection("${data.year}").document("${data.month}").collection("income").document(data.incomesDbKeys[index - 1])
    docRef.update("source", newData.source, "value", newData.value)
    println("Income edited.")
}

// Edit an expense in the database
fun editExpenseDb(db: Firestore, data: Data, index: Int, newData: Input) {
    val docRef = db.collection("${data.year}").document("${data.month}").collection("expense").document(data.incomesDbKeys[index - 1])
    docRef.update("source", newData.source, "value", newData.value)
    println("Expense edited.")
}

// Delete an income in the database
fun deleteIncomeDb(db: Firestore, data: Data, index: Int) {
    db.collection("${data.year}").document("${data.month}").collection("income").document(data.incomesDbKeys[index - 1]).delete()
    println("Income deleted.")
}

// Delete an expense in the database
fun deleteExpenseDb(db: Firestore, data: Data, index: Int) {
    db.collection("${data.year}").document("${data.month}").collection("expense").document(data.expensesDbKeys[index - 1]).delete()
    println("Expense deleted.")
}

// Calls two functions, one for the income and the other for the expense collection,
// that will delete all the data in those collections
fun deleteAll(db: Firestore, data: Data) {
    if (data.expensesDb.size < 1 && data.incomesDb.size < 1) {
        println("Nothing to delete.")
        return
    }
    println("THIS ACTION IS IRREVERSIBLE. DO YOU WISH TO CONTINUE? y/n")
    var status = readLine()!!.toLowerCase()
    while (status != "n" && status != "y") {
        println("Invalid input. Delete all data? y/n")
        status = readLine()!!.toLowerCase()
    }
    if (status == "n") {
        return
    }
    deleteAllIncomes(db, data)
    deleteAllExpenses(db, data)
    println("All data has been deleted.")
}

// Delete all incomes in the database
fun deleteAllIncomes(db: Firestore, data: Data) {
    try {
        val incomes: ApiFuture<QuerySnapshot> = db.collection("${data.year}").document("${data.month}").collection("income").get()
        val documents = incomes.get().documents
        for (document in documents) {
            document.reference.delete()
        }
    } catch (e: Exception) {
        println("Error deleting collection : " + e.message)
    }
}

// Delete all expenses in the database
fun deleteAllExpenses(db: Firestore, data: Data) {
    try {
        val expenses: ApiFuture<QuerySnapshot> = db.collection("${data.year}").document("${data.month}").collection("expense").get()
        val documents = expenses.get().documents
        for (document in documents) {
            document.reference.delete()
        }
    } catch (e: Exception) {
        println("Error deleting collection : " + e.message)
    }
}