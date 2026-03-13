package vendingmachine;

/*

Design a Vending Machine system that sells items like Coke, Pepsi, Chips, Chocolate, etc.

The vending machine should:
	1.	Display available products.
	2.	Allow users to select a product.
	3.	Accept coins/notes.
	4.	Dispense the product if enough money is inserted.
	5.	Return change if extra money is inserted.
	6.	Handle out-of-stock products.
	7.	Allow refund if the user cancels the transaction.
    Entities -
       Product , User , Denomination ,Dispenser
    ProductService
    Dispenser
    Select Product - > Vending Machine ---> checks Stock from ProductService --> if Stock Available --> Ask Payment
    VendindMachineState , VendingMachine will change
 */


import java.util.HashMap;
import java.util.Map;

interface State{

    void insertMoney(VendingMachine vendingMachine ,Currency currency);
     void selectProduct(VendingMachine vendingMachine , int slotNo , int quantity);
     void dispense(VendingMachine vendingMachine);
     void cancel(VendingMachine vendingMachine);
}

class DispenseState implements State{

    @Override
    public void insertMoney(VendingMachine vendingMachine, Currency currency) {

    }

    @Override
    public void selectProduct(VendingMachine vendingMachine, int slotNo, int quantity) {

    }

    @Override
    public void dispense(VendingMachine vendingMachine) {
        System.out.println("Dispensing ");
    }

    @Override
    public void cancel(VendingMachine vendingMachine) {

    }
}
class IdleSate implements State{

    @Override
    public void insertMoney(VendingMachine vendingMachine, Currency currency) {
        vendingMachine.balance += currency.getValue();
        System.out.println("Inserted Money: " + currency.getValue());
        vendingMachine.state = new HasMoneyState();
    }

    @Override
    public void selectProduct(VendingMachine vendingMachine, int slotNo, int quantity) {
        throw new UnsupportedOperationException("Please insert money ");
    }

    @Override
    public void dispense(VendingMachine vendingMachine) {
        throw new UnsupportedOperationException("No Product selected ");
    }

    @Override
    public void cancel(VendingMachine vendingMachine) {
        throw new UnsupportedOperationException("No process to cancel");
    }
}

class HasMoneyState implements State{
    Inventory  inventory = Inventory.getInventoryInstance();
    @Override
    public void insertMoney(VendingMachine vendingMachine, Currency currency) {
        throw new UnsupportedOperationException("Money is already loaded , please cancel to reload money");
    }

    @Override
    public void selectProduct(VendingMachine vendingMachine, int slotNo, int quantity) {
        Slot slot = inventory.getSlot(slotNo);
        if(slot.getQuantity() < quantity){
            System.out.println("Not Enough Stock Available");
        }
        if(vendingMachine.balance < quantity* slot.product.price){
            System.out.println("Not Enough Money loaded to Vending Machine");
        }
        slot.reduceStock(quantity);
    }

    @Override
    public void dispense(VendingMachine vendingMachine) {

    }

    @Override
    public void cancel(VendingMachine vendingMachine) {

    }
}


interface Currency{
    int getValue();
}

enum Coin implements Currency{
    ONE(1) ,
    TWO(2),
    FIVE(5);
    private final int value;;

    Coin(int value){
        this.value = value;
    }
    @Override
    public int getValue() {
        return value;
    }
}

enum Note implements Currency{
    TEN(10) ,
    FIFTY(50),
    HUNDRED(100);
    private final int value;;

    Note(int value){
        this.value = value;
    }
    @Override
    public int getValue() {
        return value;
    }
}

class Product{
    String name;
    int price;

    public Product(String name, int price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }


}



class VendingMachine{
    Inventory inventory = new Inventory();
    State state;
    int balance;

    void insertMoney(Currency currency){
        state.insertMoney(this , currency);
    }
    void selectProduct(VendingMachine vendingMachine , int slotNo , int quantity){
        state.selectProduct(this , slotNo , quantity);
    }
    void dispense(){
        state.dispense(this);
    }

    void cancel(){
        state.cancel(this);
    }

}

class Slot{
    int slotNumber;
    Product product;
    int quantity;

    public Slot(int slotNumber, Product product, int quantity) {
        this.slotNumber = slotNumber;
        this.product = product;
        this.quantity = quantity;
    }

    boolean isEmpty(){
        return quantity ==0;
    }
    void reduceStock(int quantity){
        this.quantity-= quantity;
    }

    public int getQuantity() {
        return quantity;
    }

}

class Inventory{
    Map<Integer , Slot> slotMap  = new HashMap<>();
    static  Inventory inventory = new Inventory();
    void addSlot(int slotNo , Product product , int quantity){
        slotMap.put(slotNo , new Slot(slotNo , product , quantity));

    }
    static Inventory getInventoryInstance(){
        return  inventory;
    }
    Slot getSlot(int slotNo){
        return  slotMap.get(slotNo);
    }
}


public class VendingMachineApp {

    public static void main(String[] args) {

    }
}
