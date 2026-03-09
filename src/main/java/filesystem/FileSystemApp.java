package filesystem;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

abstract class Node{
    protected String name;
    protected Folder parent;
    Node(String name){
        this.name = name;
    }
    Folder getParent(){
        return parent;
    }


    void setParent(Folder folder){
        this.parent = folder;
    }
    public void pwd(){
        Stack<String> stack = new Stack<>();
        Node curr = this;

        while(curr != null){
            stack.push(curr.name);
            curr = curr.parent;
        }

        while(!stack.isEmpty()){
            System.out.print("/" + stack.pop());
        }

        System.out.println();
    }
    public String getName(){
        return name;
    }
    abstract int size();
}

class Folder extends Node{
    Map<String, Node> children;
    Folder(String name) {
        super(name);
        children = new HashMap<>();
    }

    public void mkdir(String name){

        if(children.containsKey(name)){
            System.out.println("Folder already exists");
            return;
        }

        Folder folder = new Folder(name);
        folder.setParent(this);

        children.put(name, folder);
    }

    public void touch(String name){

        if(children.containsKey(name)){
            System.out.println("File already exists");
            return;
        }
        File file = new File(name);
        file.setParent(this);
        children.put(name, file);
    }


    public Folder cd(String name){
        if(name.equals("..")){
            return parent;
        }
        Node node = children.get(name);
        if(node instanceof Folder){
            return (Folder) node;
        }
        System.out.println("Invalid directory");
        return null;
    }
    public void rm(String name){
        children.remove(name);
    }
    public void mv(String source , Folder destination){

        Node node = children.remove(source);

        if(node != null){

            node.setParent(destination);
            destination.children.put(node.getName(), node);
        }
    }

    public Node find(String name){

        if(children.containsKey(name))
            return children.get(name);

        for(Node node : children.values()){

            if(node instanceof Folder){

                Node found = ((Folder) node).find(name);

                if(found != null)
                    return found;
            }
        }

        return null;
    }

    @Override
    int size(){

        int total = 0;

        for(Node node : children.values()){
            total += node.size();
        }

        return total;
    }
    public void ls() {

        for (Node node : children.values()) {

            if (node instanceof Folder)
                System.out.print(node.getName() + "/ ");
            else
                System.out.print(node.getName() + " ");
        }

        System.out.println();
    }


}
class File extends Node {

    private String content = "";

    public File(String name){
        super(name);
    }

    public void write(String content){
        this.content = content;
    }

    public String read(){
        return content;
    }

    @Override
    int size(){
        return content.length();
    }
}
class FileSystem {

    private Folder root;
    private Folder current;

    public FileSystem() {
        root = new Folder("root");
        current = root;
    }

    public void pwd() {
        current.pwd();
    }

    public void ls() {
        current.ls();
    }

    public void mkdir(String name) {
        current.mkdir(name);
    }

    public void touch(String name) {
        current.touch(name);
    }

    public void cd(String name) {

        Folder next = current.cd(name);

        if (next != null)
            current = next;
    }

    public void rm(String name) {
        current.rm(name);
    }

    public void find(String name) {

        Node node = root.find(name);

        if (node != null)
            node.pwd();
        else
            System.out.println("File not found");
    }

}

public class FileSystemApp {
    public static void main(String[] args) {

        FileSystem fs = new FileSystem();

        fs.mkdir("documents");
        fs.mkdir("photos");

        fs.ls();

        fs.cd("documents");

        fs.touch("resume.txt");
        fs.touch("notes.txt");

        fs.ls();

        fs.pwd();

        fs.cd("..");

        fs.find("resume.txt");
    }
}
