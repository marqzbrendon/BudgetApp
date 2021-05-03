import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.google.firebase.internal.NonNull
import java.io.FileInputStream
import java.math.BigDecimal


data class Income(
    @PropertyName("source") val source: String,
    @PropertyName("value") val value: Double
) {
    constructor() : this("", 0.0)
}

data class Expense(
    @PropertyName("source") val source: String,
    @PropertyName("value") val value: Double
) {
    constructor() : this("", 0.0)
}

// Mutable lists that will store the query from the database.
private var incomesFromDb = mutableListOf<Income>()
private var incomesKeys = mutableListOf<String>()
private var expensesFromDb = mutableListOf<Expense>()
private var expensesKeys = mutableListOf<String>()


//***********************************************************************
//************************** PROGRAM MAIN LOOP **************************
//***********************************************************************
fun main() {
    // My Variables
    var programActive = true

    // Initialize Database
    initializeDb()

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
            1 -> addIncomes()
            2 -> addExpenses()
            3 -> displayFinalBalance()
            4 -> deleteIncome()
            5 -> deleteExpense()
            6 -> deleteAll()
            7 -> programActive = false
        }
    }

}


//***********************************************************************
//*********************** PROGRAM BASIC FUNCTIONS ***********************
//***********************************************************************
fun addIncomes(): MutableList<Income> {
    val incomes = mutableListOf<Income>()
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
        // incomes.add(Income(source, value))
        addIncomeDb(Income(source, value))

        println("Would you like to add another income? y/n")
        status = readLine()!!.toLowerCase()
        while (status != "n" && status != "y") {
            println("Invalid input. Would you like to add another income? y/n")
            status = readLine()!!.toLowerCase()
        }
    }
    return incomes
}

fun addExpenses() {
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
        addExpenseDb(Expense(source, value))

        println("Would you like to add another expense? y/n")
        status = readLine()!!.toLowerCase()
        while (status != "n" && status != "y") {
            println("Invalid input. Would you like to add another expense? y/n")
            status = readLine()!!.toLowerCase()
        }
    }
}

private fun displayIncome(): Double {
    var incomesTotal = 0.0
    println("YOUR INCOMES:")
    for (i in incomesFromDb.indices) {
        incomesTotal += incomesFromDb[i].value
        println("${i + 1}. ${incomesFromDb[i].source} - $${incomesFromDb[i].value}")
    }
    println("INCOMES TOTAL: $${incomesTotal}")
    println("\n")
    return incomesTotal
}

private fun displayExpense(): Double {
    println("YOUR EXPENSES:")
    var expensesTotal = 0.0
    for (i in expensesFromDb.indices) {
        expensesTotal += expensesFromDb[i].value
        println("${i + 1}. ${expensesFromDb[i].source} - $${expensesFromDb[i].value}")
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

private fun getNumberOfDecimalPlaces(number: BigDecimal?): Int {
    val scale = number?.stripTrailingZeros()?.scale()
    return if (scale != null) {
        if (scale > 0) scale else 0
    } else
        0
}

fun deleteIncome() {
    if (incomesFromDb.size < 1) {
        println("There are no incomes to delete.")
        return
    }
    println("Which income would you like to delete? (Select a number from 1 to ${incomesFromDb.size})")
    displayIncome()
    var index: Int = readLine()?.toIntOrNull()!!
    while (index !in 1..incomesFromDb.size) {
        println("Invalid option. Please select a number from 1 to ${incomesFromDb.size}")
        index = readLine()?.toIntOrNull()!!
    }
    val keyToDelete = incomesKeys[index - 1]
    deleteIncomeDb(keyToDelete)
}

fun deleteExpense() {
    if (expensesFromDb.size < 1) {
        println("There are no expenses to delete.")
        return
    }
    println("Which expense would you like to delete? (Select a number from 1 to ${expensesFromDb.size})")
    displayExpense()
    var index: Int = readLine()?.toIntOrNull()!!
    while (index !in 1..expensesFromDb.size) {
        println("Invalid option. Please select a number from 1 to ${expensesFromDb.size}")
        index = readLine()?.toIntOrNull()!!
    }
    val keyToDelete = expensesKeys[index - 1]
    deleteExpenseDb(keyToDelete)
}

//***********************************************************************
//******************* COMMUNICATING WITH THE DATABASE *******************
//***********************************************************************
fun initializeDb() {
    // Fetch the service account key JSON file contents
    val serviceAccount = FileInputStream("C:/Code/Auth/budget-acd6b-firebase-adminsdk-1y2hb-3ba3808680.json")

    // Initialize the app with a service account, granting admin privileges
    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .setDatabaseUrl("https://budget-acd6b-default-rtdb.firebaseio.com")
        .build()
    FirebaseApp.initializeApp(options)

    // Retrieve current data from firebase database
    retrieveExpenseFromDb()
    retrieveIncomeFromDb()
}

fun addIncomeDb(income: Income) {
    val database = FirebaseDatabase.getInstance()
    val ref = database.getReference("data/income")
    val usersRef: DatabaseReference = ref.child("")
    usersRef.push().setValueAsync(income)
}

fun addExpenseDb(expense: Expense) {
    val database = FirebaseDatabase.getInstance()
    val ref = database.getReference("data/expense")
    val usersRef: DatabaseReference = ref.child("")
    usersRef.push().setValueAsync(expense)
}

fun deleteIncomeDb(keyToDelete: String) {
    val database = FirebaseDatabase.getInstance()
    val ref = database.getReference("data/income/$keyToDelete")
    ref.removeValueAsync()
}

fun deleteExpenseDb(keyToDelete: String) {
    val database = FirebaseDatabase.getInstance()
    val ref = database.getReference("data/expense/$keyToDelete")
    ref.removeValueAsync()
}

fun retrieveIncomeFromDb() {
    val database = FirebaseDatabase.getInstance()
    val ref = database.getReference("data/income")
    val valueEventListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(@NonNull snapshot: DataSnapshot) {
            // Loop through of each of the elements of the list (children) and convert
            // to Income objects.  Since we are receiving all of them, we need to clear our array first.
            incomesFromDb.clear()
            incomesKeys.clear()
            for (child in snapshot.children) {
                incomesFromDb.add(child.getValue(Income::class.java))
                incomesKeys.add(child.key)
            }
        }

        override fun onCancelled(@NonNull error: DatabaseError) {}
    }
    ref.child("").addValueEventListener(valueEventListener)
}

fun retrieveExpenseFromDb() {
    val database = FirebaseDatabase.getInstance()
    val ref = database.getReference("data/expense")
    val valueEventListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(@NonNull snapshot: DataSnapshot) {
            // Loop through of each of the elements of the list (children) and convert
            // to Income objects.  Since we are receiving all of them, we need to clear our array first.
            expensesFromDb.clear()
            expensesKeys.clear()
            for (child in snapshot.children) {
                expensesFromDb.add(child.getValue(Expense::class.java))
                expensesKeys.add(child.key)
            }
        }

        override fun onCancelled(@NonNull error: DatabaseError) {}
    }
    ref.child("").addValueEventListener(valueEventListener)
}

fun deleteAll() {
    if (expensesFromDb.size < 1) {
        println("Nothing to delete.")
        return
    }
    println("THIS ACTION IS IRREVERSIBLE. DO YOU WISH TO CONTINUE? y/n")
    var status = readLine()!!.toLowerCase()
    while (status != "n" && status != "y") {
        println("Invalid input. Would you like to add another income? y/n")
        status = readLine()!!.toLowerCase()
    }
    if (status == "n")
    {
        return
    }
    val database = FirebaseDatabase.getInstance()
    val ref = database.getReference("data")
    ref.removeValueAsync()
    println("All data has been deleted.")
}

