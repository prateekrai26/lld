package filesystemdemo;


import java.util.*;

interface FileSystemComponent{
    void showDetails();
    String getName();
    FileSystemComponent getParentDirectory();
    void setParentDirectory(FileSystemComponent directory);
    default void pwd(){
        FileSystemComponent temp = this.getParentDirectory();
        Stack<String> parentNames = new Stack<>();
        parentNames.push(getName());
        while(temp != null){
            parentNames.push(temp.getName());
            temp = temp.getParentDirectory();
        }
        while(!parentNames.isEmpty()){
            String parentName = parentNames.pop();
            System.out.print(parentName+"/");
        }
    }
    boolean isDirectory();
}

interface FolderComponent extends FileSystemComponent{
    Folder cd(String folderName);
    void rm(String folderName);
    void mkdir(String folderName);
    void ls();
    void touch(String fileName);
 }

class Folder implements FolderComponent{
    String name;
    Map<String, FileSystemComponent> fileSystemComponents;
    FileSystemComponent parentDirectory;
    public Folder(String name){
        fileSystemComponents = new HashMap<>();
        this.name = name;
    }
   public void ls(){
        if(fileSystemComponents.isEmpty()){
            System.out.println("No files in this folder");
        }
        for(FileSystemComponent fileSystemComponent : fileSystemComponents.values()){
            String name = fileSystemComponent.getName();
            if(fileSystemComponent.isDirectory()){
                System.out.print(name+"/ " );
            }
            else {
                System.out.print(name+"  ");
            }
        }
        System.out.println();
    }


    @Override
    public FileSystemComponent getParentDirectory() {
        return this.parentDirectory;
    }

    @Override
    public void setParentDirectory(FileSystemComponent directory) {
            this.parentDirectory = directory;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public void touch(String fileName) {
        File file = new File(fileName);
        file.setParentDirectory(this);
        fileSystemComponents.put(file.getName() , file);
    }

    @Override
    public void showDetails() {
        System.out.println("Folder name: " + name);
    }

    @Override
    public String getName() {
        return name;
    }
    @Override
    public Folder cd(String folderNAme) {
        FileSystemComponent folder = this.fileSystemComponents.get(folderNAme);
        if(folder == null){
            return null;
        }
        if(folder instanceof Folder){
            return (Folder)folder;
        }
        return null;
    }

    @Override
    public void rm(String fileSystemComponentName) {
        FileSystemComponent file = this.fileSystemComponents.get(fileSystemComponentName);
        fileSystemComponents.remove(fileSystemComponentName);

    }

    @Override
    public void mkdir(String folderName) {
        Folder folder = new Folder(folderName);
        folder.setParentDirectory(this);
        fileSystemComponents.put(folder.getName(), folder);
    }

}
class File implements FileSystemComponent{

    String fileName;
    String fileContent;
    FileSystemComponent parentDirectory;
    public File(String fileName){
        this.fileName = fileName;
    }

    void writeToFile(String fileContent){
        this.fileContent = fileContent;
    }

    @Override
    public void showDetails() {
        System.out.println("fileName : " + fileName);
    }

    @Override
    public String getName() {
        return fileName;
    }


    @Override
    public FileSystemComponent getParentDirectory() {
        return parentDirectory;
    }

    @Override
    public void setParentDirectory(FileSystemComponent directory) {
        this.parentDirectory = directory;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    String getFileContent(){
        return fileContent;
    }
}


public class FileSystemDemo {

    public static void main(String[] args) {
        Folder rootFolder = new Folder("root");
        rootFolder.touch("file1");
        rootFolder.touch("file2");
        rootFolder.touch("file3");
        rootFolder.mkdir("folder1");
        Folder folder1 = rootFolder.cd("folder1");

        folder1.touch("file5");
        folder1.ls();

        rootFolder.ls();
        folder1.pwd();

    }
}
