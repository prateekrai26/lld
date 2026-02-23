

/*
Entities/Classes

User , Expese , Group ,

Flow - User creates Account , Creates group , Add members in the group , and user can add expense in the group ->
User can checl the total balance of the group . User can checl how much he owe/owes in the group

Account , Group , Expense , Balance ,


*/


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Account{
    String id;
    String name;
    String phone;
    double balance;

    public Account(String name , String id) {
        this.name = name;
        this.balance =0;
        this.id = id;
    }
}

class Group{
    String id;
    String groupName;
    List<Account> members;
    BalanceSheet balanceSheet;
    public Group(String groupName , String id){
        this.members = new ArrayList<>();
        this.balanceSheet = new BalanceSheet();
        this.groupName = groupName;
        this.id = id;
    }
    List<Account> getMembers(){
        return this.members;
    }
}

class Expense{
    String id;
    double amount;
    String groupId;
    String userPaid;
    List<String> participants;

    public Expense(double amount, String groupId, String userPaid, ExpenseSplitStrategy expenseSplitStrategy, List<String> participants) {
        this.amount = amount;
        this.groupId = groupId;
        this.userPaid = userPaid;
        this.expenseSplitStrategy = expenseSplitStrategy;
        this.participants = participants;
    }

    ExpenseSplitStrategy expenseSplitStrategy;
}

enum SPLIT_TYPE{
    EQUAL , PERCENTAGE , VALUE
}


interface ExpenseSplitStrategy{
    Map<String , Double> splitExpense(Expense expense);
}

class EqualSplitStrategy implements ExpenseSplitStrategy{

    public Map<String , Double> splitExpense(Expense expense){
        Map<String , Double> doubleMap = new HashMap<>();
        double splitAmount = expense.amount/expense.participants.size();
        expense.participants.forEach(x -> doubleMap.put(x , splitAmount));
        return doubleMap;
    }
}

class AccountService{
    List<Account> accounts;
    static AccountService  accountService = new AccountService();
    private AccountService(){
        this.accounts = new ArrayList<>();
    }

    static  AccountService getAccountService(){
        return accountService;
    }
    void createAccount(Account account){
        accounts.add(account);
    }
    Account getAccountFromAccountId(String accountId){
        return accounts.stream().filter(a -> a.id.equals(accountId)).findFirst().get();
    }
}

class GroupService{
    List<Group> groups;
    AccountService accountService;
    static GroupService groupService = new GroupService();

    private GroupService(){
        this.groups = new ArrayList<>();
        this.accountService = AccountService.getAccountService();
    }

    static GroupService getGroupServiceInstance(){
        return groupService;
    }

    void createGroup(Group group){
        groups.add(group);
    }
    void addMemberToGroup(String groupId, String accountId){
        Account account  = accountService.getAccountFromAccountId(accountId);
        Group group = groups.stream().filter(x -> x.id.equals(groupId)).findFirst().get();
        group.members.add(account);
    }
}

class ExpenseService{
    static ExpenseService expenseService = new ExpenseService();
    GroupService groupService = GroupService.getGroupServiceInstance();
    void addExpense(Expense expense){
        Map<String , Double> participantToExpenseMap = expense.expenseSplitStrategy.splitExpense(expense);
        Group group = groupService.groups.stream().filter(x -> x.id.equals(expense.groupId)).findFirst().get();
        for(String key : participantToExpenseMap.keySet()){
            if(key.equals(expense.userPaid)) continue;
            group.balanceSheet.addDebt(expense.userPaid , key , participantToExpenseMap.get(key));
        }
    }

    static ExpenseService getExpenseServiceInstance(){
        return expenseService;
    }
}

class BalanceSheet{
    // fromUser -> (toUser -> amount)
    Map<String, Map<String, Double>> balances = new HashMap<>();

    void addDebt(String from , String toUser , double amount){
        balances.computeIfAbsent(from , k-> new HashMap<>()).merge(toUser , amount , Double::sum);
    }
}


class SpiltWiseApp{
    AccountService accountService = AccountService.getAccountService();
    GroupService groupService = GroupService.getGroupServiceInstance();
    ExpenseService expenseService = ExpenseService.getExpenseServiceInstance();

    public static void main(String[] args) {
        SpiltWiseApp spiltWiseApp = new SpiltWiseApp();
        spiltWiseApp.accountService.createAccount(new Account("prateek" , "1"));
        spiltWiseApp.accountService.createAccount(new Account("surbhi" , "2"));
        spiltWiseApp.accountService.createAccount(new Account("amit" , "3"));
        spiltWiseApp.accountService.accounts.forEach(x -> System.out.println(x.name));
        spiltWiseApp.groupService.createGroup(new Group("c401" , "1"));
        spiltWiseApp.groupService.addMemberToGroup("1" , "1");
        spiltWiseApp.groupService.addMemberToGroup("1" , "2");
        spiltWiseApp.groupService.addMemberToGroup("1" , "3");
        spiltWiseApp.expenseService.addExpense(new Expense(600 , "1", "2" , new EqualSplitStrategy() , List.of("1" ,"2" , "3")));
        System.out.println(spiltWiseApp.groupService.groups.stream().filter(x -> x.id.equals("1")).findFirst().get().balanceSheet.balances);


    }
}





