package petadoptionsystem;

/**
 *
 * @author user
 */
import java.util.Scanner;
import java.util.InputMismatchException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.*;

enum Role {
    ADMIN, STAFF
}

enum Status {
    AVAILABLE, ADOPTED
}

enum PetType {
    DOG, CAT, BIRD, RABBIT, HAMSTER
}

public class PetAdoptionSystem {

    static Scanner input = new Scanner(System.in);

    //file locations
    static final String userFilePath = "data\\users.txt";
    static final String dataFilePath = "data\\data.txt";
    static final String userAccessHistoryFilePath = "data\\userAccessHistory.txt";
    static final String adoptionHistoryFilePath = "data\\adoptionHistory.txt";
    static final String adopterListFilePath = "data\\adopterList.txt";
    static final String userChangesHistoryFilePath = "data\\userChangesHistory.txt";

    //user quantity
    static final int MAX_USERS = 10;
    static int userCount = 0;

    //user data
    static String[] userID = new String[MAX_USERS];
    static String[] username = new String[MAX_USERS];
    static String[] password = new String[MAX_USERS];
    static Role[] userRole = new Role[MAX_USERS];

    //pet quantity
    static final int MAX_PETS = 50;
    static int petCount = 0;

    //pet data
    static String[] petID = new String[MAX_PETS];
    static String[] petName = new String[MAX_PETS];
    static String[] petAge = new String[MAX_PETS];
    static PetType[] petType = new PetType[MAX_PETS];
    static String[] petBreed = new String[MAX_PETS];
    static String[] petSex = new String[MAX_PETS];
    static Status[] petStatus = new Status[MAX_PETS];

    //if isLoggedIn becomes true, method runProgram() will run
    static boolean isLoggedIn = false;

    //variable to determine if saveData() method will be called
    //avoids unnessesary filewriter saves if no changes occured
    static boolean hasChanges = false;

    //gets the id of current user to record user's acitivty
    static String idOfCurrentUser;

    public static void main(String[] args) {

        //load saved data from text files
        if (!loadFiles()) {
            return;
        }

        int loginChoice = -1;

        //keep showing login menu until user exits the program (login choice 0)
        while (loginChoice != 0) {

            //show login options
            loginMenu();

            //ask user to choose a role
            loginChoice = inputValidChoice(0, 2);

            //attempt to login
            loginChoiceHandler(loginChoice);

            //get the selected role
            Role role = getRole(loginChoice);

            //open the system menu for that role
            runProgram(role);

            //if there were changes during the session, the updated data is saved for the next session
            if (loginChoice == 0 && hasChanges) {
                saveData();
            }
        }
        //close scanner after closing program
        input.close();
    }

    //==============================================================================
    //===============================LOGIN LOGIC====================================
    //==============================================================================
    static void loginMenu() {
        System.out.println("===LOGIN MENU===");
        System.out.println("-----------------");
        System.out.println("[1] Login as Admin");
        System.out.println("[2] Login as Staff");
        System.out.println("[0] Exit program");
    }

    static int getUserInput() {
        while (true) {

            int choice;

            try {
                System.out.print("Enter Choice: ");
                choice = input.nextInt();
                input.nextLine();
                return choice;
            } catch (InputMismatchException e) {
                System.out.println("\nPlease Enter a Number\n");
                input.nextLine();
            }

        }
    }

    static int inputValidChoice(int min, int max) {
        while (true) {
            int choice = getUserInput();

            //user must only choose between a minimum and maximum number (ex. between 1 and 5 only)
            if (choice >= min && choice <= max) {
                return choice;
            }

            System.out.println("\nInvalid Choice\n");
        }
    }

    static void loginChoiceHandler(int loginChoice) {
        switch (loginChoice) {

            //calls the login method based on the chosen role,
            //then returns a boolean value to isLoggedIn varialbe
            //if login returns false, then method runProgram() will not run
            case 1:
                isLoggedIn = login(Role.ADMIN);
                break;
            case 2:
                isLoggedIn = login(Role.STAFF);
                break;
            case 0:
                System.out.println("\nYou Exited the Program\n");
                break;
        }
    }

    static boolean login(Role role) {
        //get user credentials
        String userID = inputUserID();
        String enteredUsername = inputUsername();
        String enteredPassword = inputUserPassword();

        //method checkLoginCredentials() verifies info and returns true if valid
        boolean success = checkLoginCredentials(userID, enteredUsername, enteredPassword, role);

        //if not valid, prompt incorrect message and return its boolean value
        if (!success) {
            System.out.println("\nIncorrect User Credentials\n");
        }

        return success;
    }

    static boolean checkLoginCredentials(String id, String name, String pass, Role role) {
        for (int i = 0; i < userCount; i++) {

            if (userRole[i] != role) {
                continue;
            }

            boolean correctID = id.equals(userID[i]);
            boolean correctUsername = name.equals(username[i]);
            boolean correctPassword = pass.equals(password[i]);

            if (correctID && correctUsername && correctPassword) {
                idOfCurrentUser = id;

                saveToAccessHistory(idOfCurrentUser, "LOG IN");
                return true;
            }

        }
        return false;
    }

    static Role getRole(int loginChoice) {
        switch (loginChoice) {
            case 1:
                return Role.ADMIN;
            case 2:
                return Role.STAFF;
            default:
                return null;
        }
    }

    static void logOut() {
        System.out.println("\nYou Logged Out\n");
        isLoggedIn = false;
        saveToAccessHistory(idOfCurrentUser, "LOG OUT");
    }

    //================================================================================
    //==============================USER INPUT METHODS================================
    //================================================================================
    static String inputUserID() {
        while (true) {
            System.out.print("Enter User ID: ");
            String userID = input.nextLine();

            if (!validateInput(userID, "\\d{3}")) {
                System.out.println("\nInvalid User ID Format (ID Must Be Exactly 3 Digits)\n");
                continue;
            }
            return userID;
        }
    }

    static String inputUsername() {
        System.out.print("Enter Username: ");
        return input.nextLine();
    }

    static String inputUserPassword() {
        System.out.print("Enter Password: ");
        return input.nextLine();
    }

    //================================================================================
    //===================================FILE IO======================================
    //================================================================================
    static boolean loadFiles() {
        try {
            loadUsers();
            loadData();
        } catch (FileNotFoundException e) {
            System.out.println("\nFile not found\n");
            return false;
        } catch (IOException e) {
            System.out.println("\nSomething went wrong\n");
            return false;
        }
        return true;
    }

    static void loadUsers() throws FileNotFoundException, IOException {

        //reads user text file and puts credentials to user arrays
        BufferedReader reader = new BufferedReader(new FileReader(userFilePath));

        //filereader reads each text line and assigns it to currentLine variable
        //loop resumes while there is still a line to read
        String currentLine;
        while ((currentLine = reader.readLine()) != null && userCount < MAX_USERS) {

            //split the line into parts and store it in dataParts array using "," as delimeter
            String[] dataParts = currentLine.split(",");

            //user data format
            //id,username,password,role
            if (dataParts.length == 4) {

                userID[userCount] = dataParts[0];
                username[userCount] = dataParts[1];
                password[userCount] = decrypt(dataParts[2]);
                userRole[userCount] = Role.valueOf(dataParts[3]);
                userCount++;
            }
        }
    }

    //reads pet data file and stores information into pet arrays
    static void loadData() throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(dataFilePath));

        String currentLine;

        //continue reading while there are still lines and pet array is not full
        while ((currentLine = reader.readLine()) != null && petCount < MAX_PETS) {

            //split line using comma delimiter
            String[] dataParts = currentLine.split(",");

            //pet data format:
            //(id,name,age,type,breed,sex,status)
            if (dataParts.length == 7) {

                petID[petCount] = dataParts[0];
                petName[petCount] = dataParts[1];
                petAge[petCount] = dataParts[2];
                petType[petCount] = PetType.valueOf(dataParts[3].toUpperCase());
                petBreed[petCount] = dataParts[4];
                petSex[petCount] = dataParts[5];
                petStatus[petCount] = Status.valueOf(dataParts[6].toUpperCase());
                petCount++;
            }
        }
    }

    static void saveToAdopterList(String name, String address, String contact) {
        String record = name + "," + address + "," + contact;

        if (!addLineToFile(adopterListFilePath, record)) {
            System.out.println("\nError Saving To Adopter List\n");
            return;
        }
        System.out.println("\nAdopter has been Added to List\n");
    }

    static void saveAdoptionRecord(int index, String adopterName, String adopterAddress, String adopterContact, String date) {

        String record = petID[index] + ","
                + petName[index] + ","
                + petType[index] + ","
                + adopterName + ","
                + adopterAddress + ","
                + adopterContact + ","
                + date;

        if (!addLineToFile(adoptionHistoryFilePath, record)) {
            System.out.println("\nThere Was An Error Saving Adoption Record\n");
            return;
        }
        System.out.println("\nPet Has Been Successfully Adopted\n");

        saveUserChanges("Completed a Pet Adoption", idOfCurrentUser);
        hasChanges = true;
    }

    static void saveToAccessHistory(String id, String action) {
        int index = getIndex(userID, userCount, id);

        boolean validUserIndex = index != -1 && index < userCount;

        if (!validUserIndex) {
            return;
        }

        String date = getCurrentDate();
        String record = userID[index] + "," + username[index] + "," + userRole[index] + "," + action + "," + date;

        if (!addLineToFile(userAccessHistoryFilePath, record)) {
            System.out.println("\nError Saving to Login History\n");
        }
    }

    static void saveUserChanges(String changes, String id) {
        String date = getCurrentDate();

        int index = getIndex(userID, userCount, id);

        String record = id + "," + username[index] + "," + userRole[index] + "," + changes + "," + date;

        if (!addLineToFile(userChangesHistoryFilePath, record)) {
            System.out.println("\nError Saving to History\n");
            return;
        }
        System.out.println("\nChanges have been Saved to History\n");
    }

    static void saveData() {

        //FileWriter without 'true' overwrites the old file
        //overwrite existing pet data file with updated pet list
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFilePath))) {

            //write each pet record line by line
            for (int i = 0; i < petCount; i++) {
                writer.write(petID[i] + ","
                        + petName[i] + ","
                        + petAge[i] + ","
                        + petType[i] + ","
                        + petBreed[i] + ","
                        + petSex[i] + ","
                        + petStatus[i] + "\n");
            }
            System.out.println("\nChanges During Session Has Been Saved\n");

        } catch (IOException e) {
            System.out.println("\nError During Saving Data\n");
        }

    }

    //=============================================================================
    //============================ENCRYPT AND DECRYPT==============================
    //=============================================================================
    //uses Caesar Cipher encryption with shift value of 3
    static String encrypt(String text) {
        StringBuilder result = new StringBuilder();

        //shift uppercase letters by 3 characters
        for (char c : text.toCharArray()) {
            if (Character.isUpperCase(c)) {
                char ch = (char) ((c - 'A' + 3) % 26 + 'A');
                result.append(ch);
            } else if (Character.isLowerCase(c)) {
                char ch = (char) ((c - 'a' + 3) % 26 + 'a');
                result.append(ch);
            } else {
                result.append(c); //keep spaces and symbols unchanged
            }
        }
        return result.toString();
    }

    static String decrypt(String text) {
        StringBuilder result = new StringBuilder();

        //shift lowercase letters backwards by 3 characters
        for (char c : text.toCharArray()) {
            if (Character.isUpperCase(c)) {
                char ch = (char) ((c - 'A' - 3 + 26) % 26 + 'A');
                result.append(ch);
            } else if (Character.isLowerCase(c)) {
                char ch = (char) ((c - 'a' - 3 + 26) % 26 + 'a');
                result.append(ch);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    //=============================================================================
    //==============================PROGRAM FLOW===================================
    //=============================================================================
    static void runProgram(Role role) {
        //only continue if login was successful
        if (isLoggedIn) {
            System.out.println("\nYou successfully logged in\n");
        }

        //continue showing menu until the user logs out
        while (isLoggedIn) {

            //show menu based on user role
            //and get menu option from user
            showMenu(role);
            int menuChoice = getUserInput();

            //admin and staff have different command handlers
            if (role == Role.ADMIN) {
                handleAdminCommands(menuChoice);

            } else {
                handleStaffCommands(menuChoice);
            }
        }
    }

    static void showMenu(Role role) {
        switch (role) {

            case Role.ADMIN:
                showAdminMenu();
                break;

            case Role.STAFF:
                showStaffMenu();

        }
    }

    static void showAdminMenu() {
        System.out.println("\n===ADMIN MENU===");
        System.out.println("-----------------");
        System.out.println("[1] Add New Pet");
        System.out.println("[2] View All Pets");
        System.out.println("[3] Search Pet by ID");
        System.out.println("[4] Update Pet Info");
        System.out.println("[5] Remove Pet From System");
        System.out.println("[6] Total Pets Available");
        System.out.println("[7] Total Pets Adopted");
        System.out.println("[8] Adopt Pet");
        System.out.println("[9] List of Recent Adoptions");
        System.out.println("[10] List of Adopters");
        System.out.println("[11] View All Users");
        System.out.println("[12] View User Access History");
        System.out.println("[13] View User Changes History");
        System.out.println("[0] Logout");
    }

    static void showStaffMenu() {
        System.out.println("\n===STAFF MENU===");
        System.out.println("-------------------");
        System.out.println("[1] View All Pets");
        System.out.println("[2] Search Pet By ID");
        System.out.println("[3] Update Pet Details");
        System.out.println("[4] Total Pets Available");
        System.out.println("[5] Total Pets Adopted");
        System.out.println("[6] List of Recent Adoptions");
        System.out.println("[7] List of Adopters");
        System.out.println("[0] Logout");
    }

    static void handleAdminCommands(int choice) {
        switch (choice) {
            //pet management methods
            case 1:
                addNewPet();
                break;

            case 2:
                viewAllPets();
                break;

            case 3:
                String id = prompt("Enter Pet ID to Search");
                searchPetByID(id);
                break;

            case 4:
                id = prompt("Enter Pet ID to Update");
                int index = getIndex(petID, petCount, id);
                updatePetInfo(index);
                break;

            case 5:
                id = prompt("Enter Pet ID to Remove From System");
                index = getIndex(petID, petCount, id);
                removePetFromSystem(index);
                break;

            case 6:
                System.out.println("\nNumber of Pets Avaialble: " + totalPetsAvailable() + "\n");
                break;

            case 7:
                System.out.println("\nNumber of Pets Adopted: " + totalPetsAdopted() + "\n");
                break;

            case 8:
                adoptPet();
                break;

            //history viewing methods
            case 9:
                viewRecentAdoptionList();
                break;

            case 10:
                viewAdopterList();
                break;

            case 11:
                viewAllUsers();
                break;

            case 12:
                viewUserAccessHistory();
                break;

            case 13:
                viewUserChangesHistory();
                break;

            case 0:
                logOut();
                break;

            default:
                System.out.println("\nInvalid Choice\n");

        }
    }

    static void handleStaffCommands(int choice) {
        switch (choice) {
            case 1:
                viewAllPets();
                break;

            case 2:
                String id = inputPetID();
                searchPetByID(id);
                break;

            case 3:
                id = prompt("Enter Pet ID to Update");
                int index = getIndex(petID, petCount, id);
                updatePetInfo(index);
                break;

            case 4:
                System.out.println("\nNumber of Pets Avaialble: " + totalPetsAvailable() + "\n");
                break;

            case 5:
                System.out.println("\nNumber of Pets Adopted: " + totalPetsAdopted() + "\n");
                break;

            case 6:
                viewRecentAdoptionList();
                break;

            case 7:
                viewAdopterList();
                break;

            case 0:
                logOut();
                break;

            default:
                System.out.println("\nInvalid Choice\n");

        }
    }

    //================================================================================
    //================================PET MANAGEMENT==================================
    //================================================================================
    static void addNewPet() {
        //if the list is full, prompt maximum amount message and return to menu
        if (petListIsFull()) {
            System.out.println("\nMaximum Amount of Pets Reached\n");
            return;
        }

        //get new pet details
        String id = inputPetID();
        String name = inputPetName();
        String age = inputPetAge();
        PetType type = inputPetType();
        String breed = inputPetBreed();
        String sex = inputPetSex();

        //find first empty slot in arrays
        for (int i = 0; i < MAX_PETS; i++) {
            if (petName[i] == null) {
                petID[i] = id;
                petName[i] = name;
                petAge[i] = age;
                petType[i] = type;
                petBreed[i] = breed;
                petSex[i] = sex;

                //new pets are automatically marked as available
                petStatus[i] = Status.AVAILABLE;

                //number of pets increments by 1
                petCount++;
                System.out.println("\nAdded New Pet to System\n");

                //record changes
                saveUserChanges("Added a new pet", idOfCurrentUser);
                hasChanges = true;
                return;
            }
        }
    }

    static void viewAllPets() {
        //is list is empty, prompt empty message and return to menu
        if (petListIsEmpty()) {
            System.out.println("\nPet List is Empty\n");
            return;
        }

        //display pet list in table format
        System.out.printf("%-6s%-15s%-15s%-15s%-25s%-10s%s%n", "ID:", "Name:", "Age:", "Type:", "Breed:", "Sex:", "Status:");
        System.out.println("------------------------------------------------------------------------------------------------");

        for (int i = 0; i < petCount; i++) {
            System.out.printf("%-6s", petID[i]);
            System.out.printf("%-15s", petName[i]);
            System.out.printf("%-15s", petAge[i]);
            System.out.printf("%-15s", petType[i]);
            System.out.printf("%-25s", petBreed[i]);
            System.out.printf("%-10s", petSex[i]);
            System.out.printf("%s%n", petStatus[i]);
        }
    }

    static void searchPetByID(String id) {
        //loop through pet list to find matching ID
        for (int i = 0; i < petCount; i++) {
            if (petID[i].equals(id)) {
                System.out.println("\n===PET FOUND===\n");
                showPetInfo(i);
                return;
            }
        }
        System.out.println("\nPet Not Found\n");
    }

    static void updatePetInfo(int index) {
        if (index == -1) {
            System.out.println("\nPet Not Found\n");
            return;
        }

        //shows current pet details first
        System.out.println("\nCurrent Pet Details:");
        showPetInfo(index);

        //ask user to confirm pet info update
        boolean confirmUpdatePetInfo = confirmAction("Update Pet Info?");

        //if user did not confirm, cancel and return to menu
        if (!confirmUpdatePetInfo) {
            System.out.println("\nCancelled Updating Pet\n");
            return;
        }

        System.out.println("\n(Press Enter to Skip a Field)\n");

        //calls update pet methods for each field
        petID[index] = updatePetID(petID[index]);
        petName[index] = updatePetName(petName[index]);
        petAge[index] = updatePetAge(petAge[index]);
        petType[index] = updatePetType(petType[index]);
        petBreed[index] = updatePetBreed(petBreed[index]);
        petSex[index] = updatePetSex(petSex[index]);
        System.out.println("\nPet Details Have Been Updated\n");

        //record the changes of the user to file by the type of action and user's id
        saveUserChanges("Updated Pet Info", idOfCurrentUser);
        hasChanges = true;
    }

    static void removePetFromSystem(int index) {

        //if index is not valid, print not found message and return to menu
        if (index == -1) {
            System.out.println("\nPet Not Found\n");
            return;
        }

        //show pet details first
        showPetInfo(index);

        //ask user to confrim action of removing pet
        boolean confirmPetRemoval = confirmAction("Remove Pet From System?");

        //if user did not confirm, operation is cancelled and we return to menu
        if (!confirmPetRemoval) {
            System.out.println("\nCancelled Removing Pet\n");
            return;
        }

        //removing pet from system logic:
        //we remove target index by replacing it with the next element (index[i] becomes [i+1)
        {
            for (int i = index; i < petCount - 1; i++) {
                petID[i] = petID[i + 1];
                petName[i] = petName[i + 1];
                petAge[i] = petAge[i + 1];
                petType[i] = petType[i + 1];
                petBreed[i] = petBreed[i + 1];
                petSex[i] = petSex[i + 1];
                petStatus[i] = petStatus[i + 1];
            }
        }

        //last element gets duplicated,
        //set last element to null and reduce petCount by 1
        petID[petCount - 1] = null;
        petName[petCount - 1] = null;
        petAge[petCount - 1] = null;
        petType[petCount - 1] = null;
        petBreed[petCount - 1] = null;
        petSex[petCount - 1] = null;
        petStatus[petCount - 1] = null;
        petCount--;
        System.out.println("\nPet Has Been Removed from the System\n");

        saveUserChanges("Removed a Pet from System", idOfCurrentUser);
        hasChanges = true;
    }

    //================================================================================
    //=============================PET STATISTICS=====================================
    //================================================================================
    static int totalPetsAvailable() {
        return countPetByStatus(Status.AVAILABLE);
    }

    static int totalPetsAdopted() {
        return countPetByStatus(Status.ADOPTED);
    }

    //=================================================================================
    //============================ADOPTION METHODS=====================================
    //=================================================================================
    static void adoptPet() {
        //adoption menu keeps running until user selects back
        while (true) {
            showAdoptPetMenu();
            int menuChoice = getUserInput();
            String enteredPetID;
            int petIndex;

            switch (menuChoice) {
                case 1:
                    enteredPetID = prompt("Enter Pet ID to Adopt");

                    //validate if pet exists and is still available
                    if (!validatePetForAdoption(enteredPetID)) {
                        System.out.println("\nEntered Pet ID is not Valid for Adoption\n");
                        continue;
                    }
                    searchPetByID(enteredPetID);
                    petIndex = getIndex(petID, petCount, enteredPetID);
                    completeteAdoptionProcess(petIndex);
                    break;

                case 2:
                    browseByPetType();
                    break;

                case 3:
                    viewAvailablePets();
                    break;

                case 0:
                    return;

                default:
                    System.out.println("\nPlease Choose a Valid Choice\n");
            }
        }
    }

    static void browseByPetType() {
        showBrowsePetTypeMenu();
        int menuChoice = getUserInput();

        //calls method viewByPetType() depending on the type of pet chosen
        switch (menuChoice) {
            case 1:
                viewByPetType(PetType.DOG);
                break;

            case 2:
                viewByPetType(PetType.CAT);
                break;

            case 3:
                viewByPetType(PetType.BIRD);
                break;

            case 4:
                viewByPetType(PetType.RABBIT);
                break;

            case 5:
                viewByPetType(PetType.HAMSTER);
                break;

            case 0:
                return;

            default:
                System.out.println("\nPlease Choose a Valid Choice\n");
        }
    }

    static void completeteAdoptionProcess(int petIndex) {

        //get information of adopter
        String adopterName = inputAdopterName();
        String adopterAddress = inputAdopterAddress();
        String adopterContact = inputAdopterContact();

        //ask user to confirm adoption process
        boolean confirmAdoption = confirmAction("Confirm Pet Adoption?");

        if (!confirmAdoption) {
            System.out.println("\nAdoption Process Cancelled\n");
            return;
        }

        //get the current date and time
        String adoptionDate = getCurrentDate();

        //changes pet status to adopted
        updatePetStatus(petIndex);

        saveToAdopterList(adopterName, adopterAddress, adopterContact);
        saveAdoptionRecord(petIndex, adopterName, adopterAddress, adopterContact, adoptionDate);
    }

    //===================================================================================
    //=============================DISPLAY AND VIEW METHODS==============================
    //===================================================================================
    static void showPetInfo(int index) {

        boolean validPetIndex = index != -1 && index < petCount;

        if (!validPetIndex) {
            System.out.println("\nPet Not Found\n");
            return;
        }

        System.out.println();
        System.out.println("ID: " + petID[index]);
        System.out.println("Name: " + petName[index]);
        System.out.println("Age: " + petAge[index]);
        System.out.println("Type: " + petType[index]);
        System.out.println("Breed: " + petBreed[index]);
        System.out.println("Sex: " + petSex[index]);
        System.out.println("Status: " + petStatus[index]);
        System.out.println();
    }

    static void showAdoptPetMenu() {
        System.out.println("\n===PET ADOPTION===");
        System.out.println("---------------------");
        System.out.println("[1] Adopt Pet By ID");
        System.out.println("[2] Browse By Pet Type");
        System.out.println("[3] View All Available Pets");
        System.out.println("[0] Back");
    }

    static void showBrowsePetTypeMenu() {
        System.out.println("\nChoose Type:");
        System.out.println("[1] Dog");
        System.out.println("[2] Cat");
        System.out.println("[3] Bird");
        System.out.println("[4] Rabbit");
        System.out.println("[5] Hamster");
        System.out.println("[0] Back");
    }

    static void viewByPetType(PetType type) {

        //show only pets that match the selected type and are available
        System.out.println("\nAvailable: " + type);
        System.out.println("--------------------------------------------------------------------");
        System.out.printf("%-6s%-15s%-15s%-25s%s%n", "ID:", "Name:", "Age:", "Breed:", "Sex:");
        System.out.println("--------------------------------------------------------------------");
        
        //display pets in table format filtered by type and availability
        for (int i = 0; i < petCount; i++) {
            if (type == petType[i] && petStatus[i] == Status.AVAILABLE) {
                System.out.printf("%-6s", petID[i]);
                System.out.printf("%-15s", petName[i]);
                System.out.printf("%-15s", petAge[i]);
                System.out.printf("%-25s", petBreed[i]);
                System.out.printf("%s%n", petSex[i]);
            }
        }
        System.out.println();
    }

    static void viewAvailablePets() {
        System.out.println("\n===All Available Pets=== ");
        System.out.println("---------------------------------------------------------------------------------");
        System.out.printf("%-6s%-15s%-15s%-15s%-25s%s%n", "ID:", "Name:", "Age:", "Type", "Breed:", "Sex:");
        System.out.println("---------------------------------------------------------------------------------");

        for (int i = 0; i < petCount; i++) {
            if (isAvailable(i)) {

                System.out.printf("%-6s", petID[i]);
                System.out.printf("%-15s", petName[i]);
                System.out.printf("%-15s", petAge[i]);
                System.out.printf("%-15s", petType[i]);
                System.out.printf("%-25s", petBreed[i]);
                System.out.printf("%s%n", petSex[i]);
            }
        }
        System.out.println();
    }

    static void viewAllUsers() {
        System.out.println("\n===Users in Pet Adoption System===\n");

        //views all users in the system
        //shows their credentials in a table format with encrypted passwords
        System.out.printf("%-10s%-20s%-25s%s%n", "User ID:", "Username:", "Password:", "Role:");
        System.out.println("-------------------------------------------------------------");

        for (int i = 0; i < userCount; i++) {
            System.out.printf("%-10s", userID[i]);
            System.out.printf("%-20s", username[i]);
            System.out.printf("%-25s", encrypt(password[i]));
            System.out.printf("%s%n", userRole[i]);
        }
    }

    static void viewUserAccessHistory() {
        try (BufferedReader reader = new BufferedReader(new FileReader(userAccessHistoryFilePath))) {

            System.out.println("\n===User Access History===\n");

            System.out.printf("%-10s%-15s%-15s%-15s%s%n", "User ID:", "Username:", "User Role:", "LOGIN/LOGOUT:", "Date and Time:");
            System.out.println("---------------------------------------------------------------------------------------");

            String currentLine;
            while ((currentLine = reader.readLine()) != null) {

                String[] dataParts = currentLine.split(",");

                if (dataParts.length == 5) {
                    System.out.printf("%-10s", dataParts[0]);
                    System.out.printf("%-15s", dataParts[1]);
                    System.out.printf("%-15s", dataParts[2]);
                    System.out.printf("%-15s", dataParts[3]);
                    System.out.printf("%s%n", dataParts[4]);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("\nFile not Found\n");
        } catch (IOException e) {
            System.out.println("\nError Reading File\n");
        }
    }

    static void viewRecentAdoptionList() {
        try (BufferedReader reader = new BufferedReader(new FileReader(adoptionHistoryFilePath))) {

            System.out.println("\n===Recent Pet Adoptions===\n");

            System.out.printf("%-10s%-15s%-10s%-20s%s%n", "Pet ID:", "Pet Name:", "Pet Type:", "Adopter's Name:", "Date and Time:");
            System.out.println("-------------------------------------------------------------------------------------");

            String currentLine;
            while ((currentLine = reader.readLine()) != null) {

                String[] dataParts = currentLine.split(",");

                if (dataParts.length == 7) {
                    System.out.printf("%-10s", dataParts[0]);
                    System.out.printf("%-15s", dataParts[1]);
                    System.out.printf("%-10s", dataParts[2]);
                    System.out.printf("%-20s", dataParts[3]);
                    System.out.printf("%s%n", dataParts[6]);
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("\nFile Not Found\n");
        } catch (IOException e) {
            System.out.println("\nError Reading File\n");
        }
    }

    static void viewAdopterList() {
        try (BufferedReader reader = new BufferedReader(new FileReader(adopterListFilePath))) {

            System.out.println("\n===List of Adopters===\n");

            System.out.printf("%-20s%-30s%s%n", "Name:", "Address:", "Contact:");
            System.out.println("-------------------------------------------------------------------");

            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                String[] dataParts = currentLine.split(",");

                if (dataParts.length == 3) {
                    System.out.printf("%-20s", dataParts[0]);
                    System.out.printf("%-30s", dataParts[1]);
                    System.out.printf("%s%n", dataParts[2]);
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("\nFile Not Found\n");
        } catch (IOException e) {
            System.out.println("\nError Reading File\n");
        }
    }

    static void viewUserChangesHistory() {
        try (BufferedReader reader = new BufferedReader(new FileReader(userChangesHistoryFilePath))) {

            System.out.println("\n===User Changes History===\n");

            System.out.printf("%-10s%-15s%-15s%-30s%s%n", "User ID:", "Username:", "Role:", "Changes:", "Date and Time:");
            System.out.println("--------------------------------------------------------------------------------");

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length == 5) {
                    System.out.printf("%-10s", parts[0]);
                    System.out.printf("%-15s", parts[1]);
                    System.out.printf("%-15s", parts[2]);
                    System.out.printf("%-30s", parts[3]);
                    System.out.printf("%s%n", parts[4]);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("\nFile Not Found\n");
        } catch (IOException e) {
            System.out.println("\nError Reading File\n");
        }
    }

    //=================================================================================
    //=============================PET INPUT METHODS===================================
    //=================================================================================
    static String inputPetID() {
        while (true) {
            System.out.print("Enter Pet ID: ");
            String id = input.nextLine();

            //validates the format of pet id
            if (!validateInput(id, "\\d{3}")) {
                System.out.println("\nInvalid Pet ID Format (ID Must Be Exactly 3 Digits)\n");
                continue;
            }

            //validates the value, checks if 0 or duplicate
            if (validatePetID(id)) {
                return id;
            }
        }
    }

    static String inputPetAge() {
        while (true) {

            System.out.print("Enter Age In: Month(1) || Year(2): \n");
            int choice = inputValidChoice(1, 2);

            System.out.print("Enter Pet Age: ");
            String age = input.nextLine().trim();

            //if user presses enter, age is Unknown
            if (age.isEmpty()) {
                return "Unknown";
            }

            //validate age format
            if (!validateInput(age, "\\d{1,2}")) {
                System.out.println("\nInvalid Age Format (Age Must Not Exceed 2 Digits)\n");
                continue;
            }

            //parse age to integer to validate age value, check if negative or zero
            int petAge = Integer.parseInt(age);

            //if the pet age is not valid, prompt the user again
            if (!validatePetAge(petAge)) {
                continue;
            }

            //determine whether to display Month/Months or Year/Years
            //depending on the entered age value
            switch (choice) {
                case 1:
                    if (petAge > 1) {
                        return String.valueOf(petAge) + " Months";
                    } else {
                        return String.valueOf(petAge) + " Month";
                    }
                case 2:
                    if (petAge > 1) {
                        return String.valueOf(petAge) + " Years";
                    } else {
                        return String.valueOf(petAge) + " Year";
                    }
            }
        }
    }

    static PetType inputPetType() {
        while (true) {
            System.out.println("===PET TYPES===");
            System.out.println("--------------");
            System.out.println("1. Dog");
            System.out.println("2. Cat");
            System.out.println("3. Bird");
            System.out.println("4. Rabbit");
            System.out.println("5. Hamster\n");

            int choice = getUserInput();

            //convert user menu choice into PetType enum
            switch (choice) {
                case 1:
                    return PetType.DOG;

                case 2:
                    return PetType.CAT;

                case 3:
                    return PetType.BIRD;

                case 4:
                    return PetType.RABBIT;

                case 5:
                    return PetType.HAMSTER;

                default:
                    System.out.println("\nPlease Enter a Valid Choice\n");
            }

        }

    }

    static String inputPetName() {
        return inputPetDetail("Name");
    }

    static String inputPetBreed() {
        return inputPetDetail("Breed");
    }

    static String inputPetSex() {
        return inputPetDetail("Sex");
    }

    static String inputPetDetail(String detailType) {
        //handles shared validation logic for pet name, breed, and sex
        while (true) {
            System.out.print("Enter Pet " + detailType + ": ");
            String detail = input.nextLine();

            switch (detailType) {

                case "Name":

                    //name must not be empty, prompts the user again
                    if (detail.isEmpty()) {
                        System.out.println("\nThis Field is Required\n");
                        continue;
                    }
                    break;

                case "Sex":

                    //if pet sex is empty, print required message
                    if (detail.isEmpty()) {
                        System.out.println("\nThis Field is Required\n");
                        continue;
                    }

                    //if not empty but invalid, prompt user again
                    if (!validatePetSex(detail)) {
                        System.out.println("\nInvalid Input Format\n");
                        continue;
                    }
                    break;

                default:
                    if (detail.isEmpty()) {
                        return "Unknown";
                    }
            }

            //must contain letters and spaces only
            if (!validateInput(detail, "[A-Za-z\\s]+")) {
                System.out.println("\nInvalid Input Format\n");
                continue;
            }

            return detail;

        }
    }

    //==============================================================================
    //===========================PET UPDATE METHODS==================================
    //===============================================================================
    static String updatePetID(String currentPetID) {
        while (true) {
            System.out.print("Update Pet ID (Current: " + currentPetID + "): ");
            String updatedPetID = input.nextLine().trim();

            if (updatedPetID.isEmpty()) {
                return currentPetID;
            }

            if (!validateInput(updatedPetID, "\\d{3}")) {
                System.out.println("\nInvalid ID Format (ID Must Be Excactly 3 Digits)\n");
                continue;
            }

            if (validatePetID(updatedPetID)) {
                return updatedPetID;
            }
        }
    }

    static String updatePetAge(String currentPetAge) {
        System.out.print("Current Pet Age: " + currentPetAge + "): \n");

        //ask user for confimation to update pet age through method confirmAction()
        boolean confirmAgeUpdate = confirmAction("Update Pet Age?");

        //if user did not confirm, return the current pet age
        if (!confirmAgeUpdate) {
            return currentPetAge;
        }

        //if user confirms, get and return the updated pet age
        String updatedAge = inputPetAge();

        return updatedAge;
    }

    static PetType updatePetType(PetType currentType) {
        while (true) {
            System.out.println("===PET TYPES===");
            System.out.println("--------------");
            System.out.println("1. Dog");
            System.out.println("2. Cat");
            System.out.println("3. Bird");
            System.out.println("4. Rabbit");
            System.out.println("5. Hamster");
            System.out.println("0. Skip Update");

            int choice = getUserInput();

            switch (choice) {
                case 1:
                    return PetType.DOG;

                case 2:
                    return PetType.CAT;

                case 3:
                    return PetType.BIRD;

                case 4:
                    return PetType.RABBIT;

                case 5:
                    return PetType.HAMSTER;

                case 0:
                    return currentType;

                default:
                    System.out.println("\nPlease Enter a Valid Choice\n");
            }
        }
    }

    static String updatePetName(String currentName) {
        return updatePetDetail("Name", currentName);
    }

    static String updatePetBreed(String currentBreed) {
        return updatePetDetail("Breed", currentBreed);
    }

    static String updatePetSex(String currentSex) {
        return updatePetDetail("Sex", currentSex);
    }

    static String updatePetDetail(String detailType, String currentDetail) {
        //handles shared validation logic for pet name, breed, and sex
        while (true) {
            //show the current detail
            System.out.print("Update Pet " + detailType + ": (Current: " + currentDetail + "): ");
            String newDetail = input.nextLine().trim();

            //if user pressed enter, return current detail, no changes
            if (newDetail.isEmpty()) {
                return currentDetail;
            }

            //if pet detail type is sex, validate and return pet sex
            if (detailType.equalsIgnoreCase("sex")) {
                if (!validatePetSex(newDetail)) {
                    System.out.println("\nInvalid Input Format\n");
                    continue;
                }
                return newDetail;
            }

            //if new detail is not empty, validate with regex
            //must contain letters and spcaces only
            if (!validateInput(newDetail, "[A-Za-z\\s]+")) {
                System.out.println("\nInvalid Input Format\n");
                continue;
            }
            return newDetail;
        }
    }

    static void updatePetStatus(int index) {
        //switch pet status between AVAILABLE and ADOPTED
        //used during adoption process
        if (petStatus[index] == Status.AVAILABLE) {

            petStatus[index] = Status.ADOPTED;
        } else {
            petStatus[index] = Status.AVAILABLE;
        }
    }

    //==============================================================================
    //=============================ADOPTER INPUT METHODS============================
    //==============================================================================
    static String inputAdopterName() {
        return inputAdopterDetail("Name");
    }

    static String inputAdopterAddress() {
        return inputAdopterDetail("Address");
    }

    static String inputAdopterContact() {
        return inputAdopterDetail("Contact");
    }

    static String inputAdopterDetail(String detailType) {
        //handles adopter input validation depending on detail type
        while (true) {
            System.out.print("Enter Adopter's " + detailType + ": ");
            String detail = input.nextLine().trim();

            if (detail.isEmpty()) {
                System.out.println("\nThis Field is Required\n");
                continue;
            }

            //different validation methods are used depending on the detail type
            switch (detailType) {
                case "Name":
                    if (!validateAdopterName(detail)) {
                        System.out.println("\nInvalid Input Format\n");
                        continue;
                    }
                    return detail;

                case "Address":
                    if (!validateAdopterAddress(detail)) {
                        System.out.println("\nInvalid Input Format\n");
                        continue;
                    }
                    return detail;

                case "Contact":
                    if (!validateAdopterContact(detail)) {
                        System.out.println("\nInvalid Input Format\n");
                        continue;
                    }
                    return detail;
            }
        }
    }

    //================================================================================
    //==============================HELPER METHODS====================================
    //================================================================================
    static boolean petListIsFull() {
        return petCount == MAX_PETS;
    }

    static boolean petListIsEmpty() {
        return petCount == 0;
    }

    static boolean isAvailable(int index) {
        return petStatus[index] == Status.AVAILABLE;
    }

    static boolean idIsDuplicate(String id) {
        for (int i = 0; i < petCount; i++) {
            if (petID[i].equals(id)) {
                return true;
            }
        }
        return false;
    }

    static int countPetByStatus(Status status) {
        int count = 0;

        //counts pets depending on their adoption status
        //reusable counting method for AVAILABLE and ADOPTED pets
        for (int i = 0; i < petCount; i++) {
            if (petStatus[i] == status) {
                count++;
            }
        }
        return count;
    }

    static String prompt(String message) {
        System.out.print(message + ": ");
        return input.nextLine();
    }

    static boolean confirmAction(String message) {
        //keep asking until user enters only y or n
        while (true) {
            System.out.print(message + " (y/n): ");
            String response = input.nextLine().toLowerCase().trim();

            switch (response) {
                case "y":
                    return true;

                case "n":
                    return false;

                default:
                    System.out.println("\nPlease Enter a Valid Response\n");
            }

        }
    }

    static int getIndex(String[] array, int count, String id) {
        //default value if no matching ID is found
        int index = -1;

        //search array for matching ID and return its index
        for (int i = 0; i < count; i++) {
            //return the current position once match is found
            if (array[i].equals(id)) {
                index = i;
                break;
            }
        }
        //returns -1 if ID is not found
        return index;
    }

    static String getCurrentDate() {
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String date = now.format(formatter);
        return date;
    }

    static boolean addLineToFile(String filePath, String text) {

        //FileWriter(filePath, true) enables append mode
        //new records are added without overwriting existing file contents
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {

            writer.write(text + "\n");
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    //===============================================================================
    //============================VALIDATION METHODS=================================
    //===============================================================================
    static boolean validateInput(String input, String regex) {
        //regex validator reused across multiple input methods
        return input.matches(regex);
    }

    static boolean validatePetID(String id) {

        int numericID = Integer.parseInt(id);
        if (numericID <= 0) {
            System.out.println("\nInvalid: ID Cannot Zero or Negative\n");
            return false;
        }

        if (idIsDuplicate(id)) {
            System.out.println("\nInvalid: Duplicate ID\n");
            return false;
        }

        return true;
    }

    static boolean validatePetAge(int age) {
        if (age <= 0) {
            System.out.println("\nInvalid: Age Cannot Be Negative or Zero\n");
            return false;
        }

        return true;
    }

    static boolean validatePetSex(String sex) {
        return sex.equalsIgnoreCase("male") || sex.equalsIgnoreCase("female");
    }

    static boolean validatePetForAdoption(String id) {
        //pet can only be adopted if:
        //1. the ID exists
        //2. the pet is still AVAILABLE
        for (int i = 0; i < petCount; i++) {
            if (petID[i].equals(id) && isAvailable(i)) {
                return true;
            }
        }
        return false;
    }

    static boolean validateAdopterName(String name) {
        //name must only be letters, period, commas, and spaces
        return validateInput(name, "[A-Za-z.,\\s]+");
    }

    static boolean validateAdopterAddress(String address) {
        //address must only be 1,letters, numbers, period, commas, and spaces
        return validateInput(address, "[A-Za-z0-9.,\\s]+");
    }

    static boolean validateAdopterContact(String contact) {
        //contact must start with 09 and contain 11 digits
        return validateInput(contact, "09\\d{9}");
    }
}
}
