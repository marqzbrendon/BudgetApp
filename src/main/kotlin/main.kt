import com.google.api.core.ApiFuture
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.*
import java.io.FileInputStream
import java.math.BigDecimal


data class Income(
    val source: String,
    val value: Double
)

data class Expense(
    val source: String,
    val value: Double
)

// Lists that will store the query from the database.
var incomesDb: MutableList<Income> = mutableListOf()
var expensesDb: MutableList<Expense> = mutableListOf()
var incomesDbKeys: MutableList<String> = mutableListOf()
var expensesDbKeys: MutableList<String> = mutableListOf()

//***********************************************************************
//************************** PROGRAM MAIN LOOP **************************
//***********************************************************************
fun main() {
    // My Variables
    var programActive = true

    // Initialize Database and get current data
    val db: Firestore = initializeDb()

    // Program loop
    println("Welcome! Let's take care of Business.")
    while (programActive) {
        var option: Int?
        val message = """
            SELECT AN OPTION.
            1) Add Incomes
            2) Add Expenses
            3) Display Summary
            4) Delete Income
            5) Delete Expense
            6) Delete All Data
            7) Exit Program
            """.trimMargin()
        println(message)
        option = readLine()?.toIntOrNull()
        while (option !in 1..7 || option == null) {
            println("Invalid option. Please try again:")
            option = readLine()?.toIntOrNull()
        }
        when (option) {
            1 -> addIncomes(db)
            2 -> addExpenses(db)
            3 -> displayFinalBalance()
            4 -> deleteIncome(db)
            5 -> deleteExpense(db)
            6 -> deleteAll(db)
            7 -> programActive = false
        }
    }

}


//***********************************************************************
//*********************** PROGRAM BASIC FUNCTIONS ***********************
//***********************************************************************
fun addIncomes(db: Firestore) {
    println("ADD INCOMES")
    var status = ""
    while (status != "n") {
        println("Income Source: ")
        val source = readLine().toString()

        println("Income Value: ")
        var value = readLine()?.toDoubleOrNull()
        while (value == null || getNumberOfDecimalPlaces(value.toBigDecimal()) > 2 || value < 0) {
            println("Invalid value. Please try again:")
            value = readLine()?.toDoubleOrNull()
        }
        // addIncomeDb(Income(source, value))
        addIncomeDb(db, Income(source, value))

        println("Would you like to add another income? y/n")
        status = readLine()!!.toLowerCase()
        while (status != "n" && status != "y") {
            println("Invalid input. Would you like to add another income? y/n")
            status = readLine()!!.toLowerCase()
        }
    }
}

fun addExpenses(db: Firestore) {
    println("ADD EXPENSES")
    var status = ""
    while (status != "n") {
        println("Expense Source: ")
        val source = readLine().toString()

        println("Expense Value: ")
        var value = readLine()?.toDoubleOrNull()
        while (value == null || getNumberOfDecimalPlaces(value.toBigDecimal()) > 2 || value < 0.0) {
            println("Invalid value. Please try again:")
            value = readLine()?.toDoubleOrNull()
        }
        // addExpenseDb(Expense(source, value))
        addExpenseDb(db, Expense(source, value))

        println("Would you like to add another expense? y/n")
        status = readLine()!!.toLowerCase()
        while (status != "n" && status != "y") {
            println("Invalid input. Would you like to add another expense? y/n")
            status = readLine()!!.toLowerCase()
        }
    }
}

fun displayIncome(): Double {
    var incomesTotal = 0.0
    println("YOUR INCOMES:")
    for (i in incomesDb.indices) {
        incomesTotal += incomesDb[i].value
        println("${i + 1}. ${incomesDb[i].source} - $${incomesDb[i].value}")
    }
    println("INCOMES TOTAL: $${incomesTotal}")
    println("\n")
    return incomesTotal
}

fun displayExpense(): Double {
    println("YOUR EXPENSES:")
    var expensesTotal = 0.0
    for (i in expensesDb.indices) {
        expensesTotal += expensesDb[i].value
        println("${i + 1}. ${expensesDb[i].source} - $${expensesDb[i].value}")
    }
    println("EXPENSES TOTAL: $${expensesTotal}")
    return expensesTotal
}

fun displayFinalBalance() {
    val incomeTotal = displayIncome()
    val expenseTotal = displayExpense()

    println("--------------------------------------------")
    println("FINAL BALANCE: $${incomeTotal.toBigDecimal() - expenseTotal.toBigDecimal()}")
    println("--------------------------------------------")
}

fun getNumberOfDecimalPlaces(number: BigDecimal?): Int {
    val scale = number?.stripTrailingZeros()?.scale()
    return if (scale != null) {
        if (scale > 0) scale else 0
    } else
        0
}

fun deleteIncome(db: Firestore) {
    if (incomesDb.size < 1) {
        println("There are no incomes to delete.")
        return
    }
    println("Which income would you like to delete? (Select a number from 1 to ${incomesDb.size})")
    displayIncome()
    var index: Int = readLine()?.toIntOrNull()!!
    while (index !in 1..incomesDb.size) {
        println("Invalid option. Please select a number from 1 to ${incomesDb.size}")
        index = readLine()?.toIntOrNull()!!
    }
    val keyToDelete = incomesDbKeys[index - 1]
    deleteIncomeDb(db, keyToDelete)
}

fun deleteExpense(db: Firestore) {
    if (expensesDb.size < 1) {
        println("There are no expenses to delete.")
        return
    }
    println("Which expense would you like to delete? (Select a number from 1 to ${expensesDb.size})")
    displayExpense()
    var index: Int = readLine()?.toIntOrNull()!!
    while (index !in 1..expensesDb.size) {
        println("Invalid option. Please select a number from 1 to ${expensesDb.size}")
        index = readLine()?.toIntOrNull()!!
    }
    val keyToDelete = expensesDbKeys[index - 1]
    deleteExpenseDb(db, keyToDelete)
}

//***********************************************************************
//******************* COMMUNICATING WITH THE DATABASE *******************
//***********************************************************************

fun initializeDb(): Firestore {
    // Use the application default credentials
    val serviceAccount = FileInputStream("auth_key.json")
    val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
        .setProjectId("budget-acd6b")
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build()
    retrieveAllDocuments(firestoreOptions.service)
    return firestoreOptions.service
}

fun addIncomeDb(db: Firestore, income: Income) {
    val incomeDb = HashMap<String, Any>()
    incomeDb["source"] = income.source
    incomeDb["value"] = income.value
    db.collection("income").document().set(incomeDb)
}

fun addExpenseDb(db: Firestore?, expense: Expense) {
    val expenseDb = HashMap<String, Any>()
    expenseDb["source"] = expense.source
    expenseDb["value"] = expense.value
    db?.collection("expense")?.document()?.set(expenseDb)
}

fun retrieveAllDocuments(db: Firestore?) {
    db?.collection("income")
        ?.addSnapshotListener(object : EventListener<QuerySnapshot?> {
            override fun onEvent(snapshots: QuerySnapshot?, e: FirestoreException?) {
                incomesDb.clear()
                incomesDbKeys.clear()
                if (e != null) {
                    System.err.println("Listen failed:$e")
                    return
                }
                if (snapshots != null) {
                    for (doc in snapshots) {
                        doc.getString("source")?.let {
                            doc.getDouble("value")
                                ?.let { it1 -> Income(it, it1) }
                        }?.let { incomesDb.add(it) }
                        incomesDbKeys.add(doc.id)
                    }
                }
            }
        })

    db?.collection("expense")
        ?.addSnapshotListener(object : EventListener<QuerySnapshot?> {
            override fun onEvent(snapshots: QuerySnapshot?, e: FirestoreException?) {
                expensesDb.clear()
                expensesDbKeys.clear()
                if (e != null) {
                    System.err.println("Listen failed:$e")
                    return
                }
                if (snapshots != null) {
                    for (doc in snapshots) {
                        doc.getString("source")?.let {
                            doc.getDouble("value")
                                ?.let { it1 -> Expense(it, it1) }
                        }?.let { expensesDb.add(it) }
                        expensesDbKeys.add(doc.id)
                    }
                }
            }
        })
}

fun deleteIncomeDb(db:Firestore ,keyToDelete: String) {
    db.collection("income").document(keyToDelete).delete()
    println("Income deleted.")
}

fun deleteExpenseDb(db: Firestore, keyToDelete: String) {
    db.collection("expense").document(keyToDelete).delete()
    println("Expense deleted.")
}

fun deleteAll(db: Firestore) {
    if (expensesDb.size < 1 && incomesDb.size < 1) {
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
    deleteAllIncomes(db)
    deleteAllExpenses(db)
    println("All data has been deleted.")
}

fun deleteAllIncomes(db: Firestore) {
    try {
        val incomes: ApiFuture<QuerySnapshot> = db.collection("income").get()
        val documents = incomes.get().documents
        for (document in documents) {
            document.reference.delete()
        }
    } catch (e: Exception) {
        System.err.println("Error deleting collection : " + e.message)
    }
}

fun deleteAllExpenses(db: Firestore) {
    try {
        val expenses: ApiFuture<QuerySnapshot> = db.collection("expense").get()
        val documents = expenses.get().documents
        for (document in documents) {
            document.reference.delete()
        }
    } catch (e: Exception) {
        System.err.println("Error deleting collection : " + e.message)
    }
}

