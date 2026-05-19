/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
    DOG, CAT, BIRD, RABBIT, HAMSTER, PIG, SHEEP, SNAKE, TARANTULA, CARABAO, FISH, TURTLE, TIGER, LION
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
    static int[] userAge = new int[MAX_USERS];
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
    static String currentUsername;

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
            loginChoice = inputValidChoice(0, 3);

            //attempt to login
            loginChoiceHandler(loginChoice);

            //get the selected role
            Role role = getRole(loginChoice);

            //open the system menu for that role
            runProgram(role);

            //if there were changes during the session, the updated data is saved for the next session
            if (loginChoice == 0 && hasChanges) {
                saveData();
                saveUsers();
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
        System.out.println("[3] Register");
        System.out.println("[0] Exit program");
    }

    //validate number input
    static int getUserInput() {
        while (true) {

            try {
                System.out.print("Enter Choice: ");
                double num = input.nextDouble();
                input.nextLine();

                if (num < 0) {
                    System.out.println("\nNumber Must not be Negative\n");
                } else if (num % 1 == 0) {
                    int userInput = (int) num;
                    return userInput;
                } else {
                    System.out.println("\nEnter a Valid Value\n");
                }
            } catch (InputMismatchException e) {
                System.out.println("\nPlease Enter a Number\n");
                input.nextLine();
            }
        }
    }

    //validate choice (if number is valid but not choice)
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

    static boolean login(Role role) {
        //get user credentials
        String enteredUsername = inputUsername();
        String enteredPassword = inputUserPassword();

        //method checkLoginCredentials() verifies info and returns true if valid
        boolean success = checkLoginCredentials(enteredUsername, enteredPassword, role);

        //if not valid, prompt incorrect message and return its boolean value
        if (!success) {
            System.out.println("\nIncorrect User Credentials\n");
        }

        return success;
    }

    static boolean checkLoginCredentials(String name, String pass, Role role) {
        //loop through all registered users
        for (int i = 0; i < userCount; i++) {

            int userIndex;

            //skip users that do not match the selected role
            if (userRole[i] != role) {
                continue;
            }

            //store current user position
            userIndex = i;

            //if both username and password are correct,
            //save current user info and allow login
            boolean correctUsername = name.equals(username[userIndex]);
            boolean correctPassword = pass.equals(password[userIndex]);

            if (correctUsername && correctPassword) {
                idOfCurrentUser = userID[userIndex];
                currentUsername = username[userIndex];

                saveToAccessHistory(idOfCurrentUser, "LOG IN");
                return true;
            }

        }
        return false;
    }

    static void loginChoiceHandler(int loginChoice) {
        switch (loginChoice) {

            //calls the login method based on the chosen role,
            //which returns a boolean value to isLoggedIn varialbe
            //if login returns false, then method runProgram() will not run
            case 1:
                isLoggedIn = login(Role.ADMIN);
                break;

            case 2:
                isLoggedIn = login(Role.STAFF);
                break;

            case 3:
                registerUser();
                break;

            case 0:
                System.out.println("\nYou Exited the Program\n");
                break;
        }
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
    //==============================REGISTRATION LOGIC================================
    //================================================================================
    static void registerUser() {

        //get new User credentials
        String createdUserID = generateUserID();
        String createdUsername = createUsername();
        String createdPassword = createPassword();
        int createdAge = inputUserAge();

        //put credentials into users array
        userID[userCount] = createdUserID;
        username[userCount] = createdUsername;
        password[userCount] = createdPassword;
        userAge[userCount] = createdAge;
        userRole[userCount] = Role.STAFF;
        userCount++;

        System.out.println("\nRegisration Successful\n");
        saveUsers();
    }

    //================================================================================
    //==============================USER INPUT METHODS================================
    //================================================================================
    static String inputUsername() {
        return requiredPrompt("Enter Username");
    }

    static String inputUserPassword() {
        return requiredPrompt("Enter Password");
    }

    static String generateUserID() {
        //outer loop check IDs from 001 to 999
        for (int i = 1; i <= 999; i++) {
            //format number to string with 0 padding and 3 width
            String newID = String.format("%03d", i);

            boolean idExists = false;

            //inner loop check if current ID value already exists in userID[]
            for (int j = 0; j < userCount; j++) {

                if (userID[j].equals(newID)) {
                    idExists = true;
                    break;
                }
            }
            //if ID does not exist, return it
            if (!idExists) {
                return newID;
            }
        }
        //if all IDs are used
        return null;
    }

    static String createUsername() {
        while (true) {
            String enteredUsername = requiredPrompt("Create Username");

            //username must only contain letters, numbers, dots, underscores, or hyphens
            //and must be between 3 to 20 characters long
            boolean validUsername = validateInput(enteredUsername, "(?=.*[A-Za-z])[A-Za-z0-9 ]{3,20}");
            boolean usernameIsDuplicate = false;
            
            for (int i = 0; i < userCount; i++){
                if (username[i].equals(enteredUsername)){
                    usernameIsDuplicate  =true;
                }
            }
            
            if (usernameIsDuplicate){
                System.out.println("\nThis Username is Already Being Used\n");
                continue;
            }

            if (!validUsername) {
                System.out.println("\nInvalid Username\n");
                continue;
            }
            return enteredUsername;
        }
    }

    static String createPassword() {
        while (true) {
            String password = requiredPrompt("Create Password");

            //password must contain:
            //1 lowercase letter
            //1 uppercase letter
            //1 number
            //1 special character
            //minimum length of 8 characters
            boolean validPassword = validateInput(password, "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}");

            if (!validPassword) {
                System.out.println("\nInvalid Password");
                System.out.println("Password should contain at least 8 characters,");
                System.out.println("lower and uppercase letters, numbers, and symbols\n");
                continue;
            }

            String confirmPass = requiredPrompt("Confirm Password");

            while (true) {
                if (!confirmPass.equals(password)) {
                    System.out.println("\nPassword does not match\n");
                    continue;
                }
                return password;
            }
        }
    }

    static int inputUserAge() {
        while (true) {

            try {
                System.out.print("Enter User Age (18-60):");
                double age = input.nextDouble();

                //validate age range and ensure value is a whole number
                if (!validateUserAge(age)) {
                    continue;
                }
                int userAge = (int) age;
                return userAge;
            } catch (InputMismatchException e) {
                System.out.println("\nPlease Enter a Number\n");
                input.nextLine(); //clear newline
            }
        }
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
            System.out.println("\nSomething Went Wrong Loading Files\n");
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
            String[] dataParts = currentLine.split("\\|");

            //user data format
            //id,username,password,role
            if (dataParts.length == 5) {

                userID[userCount] = dataParts[0];
                username[userCount] = dataParts[1];
                userAge[userCount] = Integer.parseInt(dataParts[2]);
                //convert encrypted password back to normal text before storing
                password[userCount] = decrypt(dataParts[3]);
                userRole[userCount] = Role.valueOf(dataParts[4]);
                userCount++;
            }
        }
        reader.close();
    }

    //reads pet data file and stores information into pet arrays
    static void loadData() throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(dataFilePath));

        String currentLine;

        //continue reading while there are still lines and pet array is not full
        while ((currentLine = reader.readLine()) != null && petCount < MAX_PETS) {

            //split line using comma delimiter
            String[] dataParts = currentLine.split("\\|");

            //pet data format:
            //(id,name,age,type,breed,sex,status)
            if (dataParts.length == 7) {

                petID[petCount] = dataParts[0];
                petName[petCount] = dataParts[1];
                petAge[petCount] = dataParts[2];
                //convert text from file into enum values
                petType[petCount] = PetType.valueOf(dataParts[3].toUpperCase());
                petBreed[petCount] = dataParts[4];
                petSex[petCount] = dataParts[5];
                //convert saved status text into Status enum
                petStatus[petCount] = Status.valueOf(dataParts[6].toUpperCase());
                petCount++;
            }
        }
        reader.close();
    }

    static void saveToAdopterList(String name, String address, String contact) {
        String record = name + "|" + address + "|" + contact;

        if (!addLineToFile(adopterListFilePath, record)) {
            System.out.println("\nError Saving To Adopter List\n");
            return;
        }
        System.out.println("\nAdopter has been Added to List\n");
    }

    static void saveAdoptionRecord(int index, String adopterName, String adopterAddress, String adopterContact, String date) {

        //store adoption details into one formatted record
        String record = petID[index] + "|"
                + petName[index] + "|"
                + petType[index] + "|"
                + adopterName + "|"
                + adopterAddress + "|"
                + adopterContact + "|"
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
        //find the index of the current user using their ID
        int index = getIndex(userID, userCount, id);

        //check if index exists and is within array range
        boolean validUserIndex = index != -1 && index < userCount;

        if (!validUserIndex) {
            return;
        }

        String date = getCurrentDate();
        String record = userID[index] + "|" + username[index] + "|" + userRole[index] + "|" + action + "|" + date;

        if (!addLineToFile(userAccessHistoryFilePath, record)) {
            System.out.println("\nError Saving to Login History\n");
        }
    }

    static void saveUserChanges(String changes, String id) {
        String date = getCurrentDate();

        int index = getIndex(userID, userCount, id);

        //record the type of change made by the user with timestamp
        String record = id + "|" + username[index] + "|" + userRole[index] + "|" + changes + "|" + date;

        if (!addLineToFile(userChangesHistoryFilePath, record)) {
            System.out.println("\nError Saving to History\n");
            return;
        }
        System.out.println("\nChanges have been Saved to History\n");
    }

    static void saveData() {

        //overrwites data.txt with updated pet list
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFilePath))) {

            //save every pet currently stored in arrays into the file
            for (int i = 0; i < petCount; i++) {

                String record = petID[i] + "|"
                        + petName[i] + "|"
                        + petAge[i] + "|"
                        + petType[i] + "|"
                        + petBreed[i] + "|"
                        + petSex[i] + "|"
                        + petStatus[i] + "\n";

                writer.write(record);
            }
            System.out.println("\nChanges During Session Has Been Saved\n");

        } catch (IOException e) {
            System.out.println("\nError During Saving Pet Data\n");
        }

    }

    static void saveUsers() {

        //overwrites existing user data file with updated user list
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(userFilePath))) {

            for (int i = 0; i < userCount; i++) {

                String record = userID[i] + "|"
                        + username[i] + "|"
                        + userAge[i] + "|"
                        + encrypt(password[i]) + "|"
                        + userRole[i] + "\n";

                writer.write(record);
            }
        } catch (IOException e) {
            System.out.println("\nError Saving Users\n");
        }

    }

    //=============================================================================
    //============================ENCRYPT AND DECRYPT==============================
    //=============================================================================
    //uses Caesar Cipher encryption with shift value of 3
    static String encrypt(String text) {
        StringBuilder result = new StringBuilder();

        //loop through every character in the text
        for (char c : text.toCharArray()) {

            //shift letters by 3 characters
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

        for (char c : text.toCharArray()) {

            //reverse the Caesar Cipher shift by moving letters backward
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
        //print welcome message if login was successful
        if (isLoggedIn) {
            System.out.println("\nWelcome " + currentUsername + "\n");
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
                //find pet index using entered ID before updating/removing
                int index = getIndex(petID, petCount, id);
                updatePetInfo(index);
                break;

            case 5:
                id = prompt("Enter Pet ID to Remove From System");
                //find pet index using entered ID before updating/removing
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
                String id = prompt("Enter Pet ID to Search");
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
        String id = generatePetID();
        String name = inputPetName();
        String age = inputPetAge();
        PetType type = inputPetType();
        String breed = inputPetBreed();
        String sex = inputPetSex();

        //store new pet information in the first available empty slot
        for (int i = 0; i < MAX_PETS; i++) {
            if (petName[i] == null) {
                petID[i] = id;
                petName[i] = name;
                petAge[i] = age;
                petType[i] = type;
                petBreed[i] = breed;
                petSex[i] = sex;

                //ask for confirmation before adding pet
                if (!confirmAction("Confirm Adding Pet?")) {
                    System.out.println("\nCancelled Adding Pet\n");
                    return;
                }

                //new pets are automatically marked as available
                petStatus[i] = Status.AVAILABLE;

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

        //loop through all pets and display their information row by row
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
        //compare entered ID with every pet ID in the system
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

        //allow user to update fields one by one
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

        //shift all elements left to overwrite removed pet
        for (int i = index; i < petCount - 1; i++) {
            petID[i] = petID[i + 1];
            petName[i] = petName[i + 1];
            petAge[i] = petAge[i + 1];
            petType[i] = petType[i + 1];
            petBreed[i] = petBreed[i + 1];
            petSex[i] = petSex[i + 1];
            petStatus[i] = petStatus[i + 1];
        }

        //clear duplicated last slot after shifting elements
        int lastElement = petCount - 1;

        petID[lastElement] = null;
        petName[lastElement] = null;
        petAge[lastElement] = null;
        petType[lastElement] = null;
        petBreed[lastElement] = null;
        petSex[lastElement] = null;
        petStatus[lastElement] = null;
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
     static void showAdoptPetMenu() {
        System.out.println("\n===PET ADOPTION===");
        System.out.println("---------------------");
        System.out.println("[1] Adopt Pet By ID");
        System.out.println("[2] Browse By Pet Type");
        System.out.println("[3] View All Available Pets");
        System.out.println("[0] Back");
    }
     
    static void adoptPet() {
        //adoption menu keeps running until user selects back
        while (true) {
            showAdoptPetMenu();
            int menuChoice = getUserInput();

            //store entered pet ID
            String enteredPetID;

            //get the array position of the selected pet
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
    
     static void showBrowsePetTypeMenu() {
        System.out.println("\nChoose Type:");
        System.out.println("[1] Dog");
        System.out.println("[2] Cat");
        System.out.println("[3] Bird");
        System.out.println("[4] Rabbit");
        System.out.println("[5] Hamster");
        System.out.println("[6] Pig");
        System.out.println("[7] Sheep");
        System.out.println("[8] Snake");
        System.out.println("[9] Tarantula");
        System.out.println("[10] Carabao");
        System.out.println("[11] Fish");
        System.out.println("[12] Turtle");
        System.out.println("[13] Tiger");
        System.out.println("[14] Lion");
        System.out.println("[0] Back");
    }

    static void browseByPetType() {
        //display pet type menu and get user's selected type
        showBrowsePetTypeMenu();
        int menuChoice = getUserInput();

        //display only pets that belong to the selected type
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
                
            case 6:
                viewByPetType(PetType.PIG);
                break;
                
            case 7:
                viewByPetType(PetType.SHEEP);
                break;
                
            case 8:
                viewByPetType(PetType.SNAKE);
                break;
                
            case 9:
                viewByPetType(PetType.TARANTULA);
                break;
                
            case 10:
                viewByPetType(PetType.CARABAO);
                break;
                
            case 11:
                viewByPetType(PetType.FISH);
                break;
                
            case 12:
                viewByPetType(PetType.TURTLE);
                break;
                
            case 13:
                viewByPetType(PetType.TIGER);
                break;
                
            case 14:
                viewByPetType(PetType.LION);

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

        //save current date and time as adoption record timestamp
        String adoptionDate = getCurrentDate();

        //mark selected pet as ADOPTED
        updatePetStatus(petIndex);

        //save adopter and adoption record
        saveToAdopterList(adopterName, adopterAddress, adopterContact);
        saveAdoptionRecord(petIndex, adopterName, adopterAddress, adopterContact, adoptionDate);
    }

    //===================================================================================
    //=============================DISPLAY AND VIEW METHODS==============================
    //===================================================================================
    static void showPetInfo(int index) {

        //validate first if pet index exists in the array
        boolean validPetIndex = index != -1 && index < petCount;

        if (!validPetIndex) {
            System.out.println("\nPet Not Found\n");
            return;
        }

        //display complete information of selected pet
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


    static void viewByPetType(PetType type) {
        //show pets only if:
        //1. pet type matches selected type
        //2. pet is still available for adoption
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
        //display only pets whose status is AVAILABLE
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

    static void viewUserAccessHistory() {
        try (BufferedReader reader = new BufferedReader(new FileReader(userAccessHistoryFilePath))) {

            System.out.println("\n===User Access History===\n");

            System.out.printf("%-10s%-15s%-15s%-15s%s%n", "User ID:", "Username:", "User Role:", "LOGIN/LOGOUT:", "Date and Time:");
            System.out.println("---------------------------------------------------------------------------------------");

            //read access history file line by line
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {

                //split saved text record into separate values, using "|" as delimiter
                String[] dataParts = currentLine.split("\\|");

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

            //display adoption records stored in adoption history file
            System.out.println("\n===Recent Pet Adoptions===\n");

            System.out.printf("%-10s%-15s%-10s%-20s%s%n", "Pet ID:", "Pet Name:", "Pet Type:", "Adopter's Name:", "Date and Time:");
            System.out.println("-------------------------------------------------------------------------------------");

            String currentLine;
            while ((currentLine = reader.readLine()) != null) {

                String[] dataParts = currentLine.split("\\|");

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

            //display all adopters saved in adopter list file
            System.out.println("\n===List of Adopters===\n");

            System.out.printf("%-20s%-30s%s%n", "Name:", "Address:", "Contact:");
            System.out.println("-------------------------------------------------------------------");

            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                String[] dataParts = currentLine.split("\\|");

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

            //display all recorded changes made by users
            System.out.println("\n===User Changes History===\n");

            System.out.printf("%-10s%-15s%-15s%-30s%s%n", "User ID:", "Username:", "Role:", "Changes:", "Date and Time:");
            System.out.println("------------------------------------------------------------------------------------------");

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");

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
    static String generatePetID() {

        //generate IDs from 001 to 999
        for (int i = 1; i <= 999; i++) {
            String newID = String.format("%03d", i);

            //check if generated ID already exists in pet list
            boolean idExists = false;
            for (int j = 0; j < petCount; j++) {

                if (petID[j].equals(newID)) {
                    idExists = true;
                    break;
                }
            }
            if (!idExists) {
                return newID;
            }
        }

        return null;
    }

    static String inputPetAge() {
        while (true) {

            //user chooses whether age will be measured in months or years
            System.out.print("Enter Age In: Month(1) || Year(2): \n");
            int choice = inputValidChoice(1, 2);

            System.out.print("Enter Pet Age: ");
            String age = input.nextLine().trim();

            //empty input means pet age is unknown
            if (age.isEmpty()) {
                return "Unknown";
            }
            
            double petAgeDouble;
            
            //check if input is numeric
            try{
                petAgeDouble = Double.parseDouble(age);
            } catch (NumberFormatException e){
                System.out.println("\nEnter a Valid Number\n");
                continue;
            }
            
            //reject decimals like 1.5 or 5.5
            if (petAgeDouble % 1 != 0){
                System.out.println("\nAge Must Be a Whole Number\n");
                continue;
            }
         
            //convert double petAge into integer for validation
            int petAge = (int) petAgeDouble;

            //validate range 1-99
            if (!validatePetAge(petAge)) {
                continue;
            }

            //format age properly with singular/plural labels
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
            System.out.println("5. Hamster");
            System.out.println("6. Pig");
            System.out.println("7. Sheep");
            System.out.println("8. Snake");
            System.out.println("9. Tarantula");
            System.out.println("10. Carabao");
            System.out.println("11. Fish");
            System.out.println("12. Tutle");
            System.out.println("13. Tiger");
            System.out.println("14. Lion");

            int choice = getUserInput();

            //convert menu choice into matching PetType enum
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

                case 6:
                    return PetType.PIG;

                case 7:
                    return PetType.SHEEP;

                case 8:
                    return PetType.SNAKE;

                case 9:
                    return PetType.TARANTULA;

                case 10:
                    return PetType.CARABAO;

                case 11:
                    return PetType.FISH;

                case 12:
                    return PetType.TURTLE;

                case 13:
                    return PetType.TIGER;

                case 14:
                    return PetType.LION;

                default:
                    System.out.println("\nPlease Enter a Valid Choice\n");
            }

        }

    }

    static String inputPetName() {
        while (true) {
            String petName = requiredPrompt("Enter Pet Name");

            //pet name must only contain letters, spaces, commas, or periods
            if (!validatePetName(petName)) {
                System.out.println("\nInvalid Name Format\n");
                continue;
            }
            return petName;
        }

    }

    static String inputPetBreed() {
        while (true) {
            String petBreed = prompt("Enter Pet Breed (Enter to Skip)");

            //allow blank breed input if user wants to skip
            if (petBreed.isEmpty()) {
                return "Unknown";
            }

            if (!validateInput(petBreed, "[A-Za-z\\s]+")) {
                System.out.println("\nInvalid Input\n");
                continue;
            }
            return petBreed;
        }
    }

    static String inputPetSex() {
        while (true) {
            String petSex = requiredPrompt("Enter Pet Sex (m/f)");

            //convert to lowercase
            petSex = petSex.toLowerCase();

            //convert shortcut m/f input into complete words
            switch (petSex) {
                case "m":
                    return "Male";

                case "f":
                    return "Female";

                default:
                    System.out.println("\nEnter a Valid Choice\n");
            }
        }
    }

    //==============================================================================
    //===========================PET UPDATE METHODS==================================
    //===============================================================================
    static String updatePetAge(String currentPetAge) {
        System.out.print("Current Pet Age: (Current Pet Age: " + currentPetAge + "): \n");

        //ask user for confimation to update pet age through method confirmAction()
        boolean confirmAgeUpdate = confirmAction("Update Pet Age?");

        //keep existing age if user cancels update
        if (!confirmAgeUpdate) {
            return currentPetAge;
        }

        //if user confirms, get and return the updated pet age
        String updatedAge = inputPetAge();

        return updatedAge;
    }

    static PetType updatePetType(PetType currentType) {
        while (true) {
            System.out.println("\nUpdate Pet Type");
            System.out.println("1. Dog");
            System.out.println("2. Cat");
            System.out.println("3. Bird");
            System.out.println("4. Rabbit");
            System.out.println("5. Hamster");
            System.out.println("6. Pig");
            System.out.println("7. Sheep");
            System.out.println("8. Snake");
            System.out.println("9. Tarantula");
            System.out.println("10. Carabao");
            System.out.println("11. Fish");
            System.out.println("12. Tutle");
            System.out.println("13. Tiger");
            System.out.println("14. Lion");
            //allow user to skip updating pet type
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

                case 6:
                    return PetType.PIG;

                case 7:
                    return PetType.SHEEP;

                case 8:
                    return PetType.SNAKE;

                case 9:
                    return PetType.TARANTULA;

                case 10:
                    return PetType.CARABAO;

                case 11:
                    return PetType.FISH;

                case 12:
                    return PetType.TURTLE;

                case 13:
                    return PetType.TIGER;

                case 14:
                    return PetType.LION;

                case 0:
                    return currentType;

                default:
                    System.out.println("\nPlease Enter a Valid Choice\n");
            }
        }
    }

    static String updatePetName(String currentPetName) {
        while (true) {
            String newPetName = prompt("Update Pet Name (Current: " + currentPetName + ")");

            //keep current pet name if input is blank
            if (newPetName.isEmpty()) {
                return currentPetName;
            }

            if (!validatePetName(newPetName)) {
                System.out.println("Invalid Name format");
                continue;
            }
            return newPetName;
        }
    }

    static String updatePetBreed(String currentPetBreed) {
        while (true) {
            String newPetBreed = prompt("Update Pet Breed (Current: " + currentPetBreed + ")");

            //keep current breed if user skips update
            if (newPetBreed.isEmpty()) {
                return currentPetBreed;
            }

            if (!validateInput(newPetBreed, "[A-Za-z\\s]+")) {
                System.out.println("Invalid Name format");
                continue;
            }
            return newPetBreed;
        }
    }

    static String updatePetSex(String currentPetSex) {
        while (true) {
            String newPetSex = prompt("Update Pet Sex (m/f)(Current: " + currentPetSex + ")");

            //keep current pet sex if user skips update
            if (newPetSex.isEmpty()) {
                return currentPetSex;
            }

            switch (newPetSex) {
                case "m":
                    return "Male";

                case "f":
                    return "Female";

                default:
                    System.out.println("\nPlease Enter a Valid Choice\n");
            }
        }
    }

    static void updatePetStatus(int index) {
        //toggle pet status between AVAILABLE and ADOPTED
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
        while (true) {
            System.out.println("===Enter Adopter Full Name===");
            String firstName = requiredPrompt("First Name");
            String middleInitial = prompt("Middle Initial (Enter to Skip)");
            String lastName = requiredPrompt("Last Name");

            //validate full name parts before combining them
            if (!validateName(firstName, middleInitial, lastName)) {
                System.out.println("\nInvalid Name Format\n");
                continue;
            }
            //include middle initial only if user entered one
            String fullName;
            if (!middleInitial.isEmpty()) {
                fullName = firstName + " " + middleInitial + " " + lastName;

            } else {
                fullName = firstName + " " + lastName;
            }

            return fullName;
        }
    }

    static String inputAdopterAddress() {
        while (true) {
            System.out.println("\n===Enter Adopter's Address=== (Street, Barangay, City)");
            String street = requiredPrompt("Street");
            String barangay = requiredPrompt("Barangay");
            String city = requiredPrompt("City");

            //validate each address part before combining
            if (!validateAdopterAddress(street, barangay, city)) {
                System.out.println("\nInvalid Address Format\n");
                continue;
            }
            //combine address parts into one formatted string
            String address = street + ", " + barangay + ", " + city;
            return address;
        }
    }

    static String inputAdopterContact() {
        while (true) {
            String contact = requiredPrompt("\nEnter Adopter's Contact No.(ex. 09453524992)");

            if (!validateAdopterContact(contact)) {
                System.out.println("\nInvalid Contact Format. Contact must start with 09 and contain 11 digits\n");
                continue;
            }
            return contact;
        }
    }

    //===============================================================================
    //=================================USER  MANAGEMENT==============================
    //===============================================================================
    static boolean manageStaff() {
        while (true) {
            //admin can either promote or remove staff accounts
            System.out.println("\n1. Promote a Staff");
            System.out.println("2. Remove a Staff");
            System.out.println("3. Return to Menu");
            int choice = inputValidChoice(1, 3);

            switch (choice) {
                case 1:
                    String id = prompt("Enter Staff ID to Promote to Admin");
                    int index = getIndex(userID, userCount, id);
                    promoteStaffToAdmin(index);
                    break;

                case 2:
                    id = prompt("Enter Staff ID to Remove From System");
                    index = getIndex(userID, userCount, id);
                    removeStaffFromSystem(index);
                    break;

                case 3:
                    return false;

                default:
                    System.out.println("\nInvalid Choice\n");
            }
        }
    }

    static void removeStaffFromSystem(int index) {
        //if staff index is not valid, print not found message and return to menu
        boolean validUserIndex = index == -1 || index > userCount;

        if (validUserIndex) {
            System.out.println("\nStaff Not Found\n");
            return;
        }

        //prevent admins from being removed from the system
        if (userRole[index] != null && userRole[index] == Role.ADMIN) {
            System.out.println("\nCannot Remove Admins\n");
            return;
        }

        //if index is valid and is staff, show user info
        showUserInfo(index);

        //asks for confirmation
        if (!confirmAction("Remove " + username[index] + "?")) {
            System.out.println("Cancelled Removing Staff");
            return;
        }

        //get staff's username for completed message
        String staffUsername = username[index];

        //shift all users left to overwrite removed staff account
        for (int i = index; i < userCount - 1; i++) {
            userID[i] = userID[i + 1];
            username[i] = username[i + 1];
            userAge[i] = userAge[i + 1];
            password[i] = password[i + 1];
            userRole[i] = userRole[i + 1];
        }

        //set last element to null and decrement userCount
        int lastElement = userCount - 1;

        userID[lastElement] = null;
        username[lastElement] = null;
        userAge[lastElement] = 0;
        password[lastElement] = null;
        userRole[lastElement] = null;
        userCount--;

        //record the changes
        System.out.println("Staff " + staffUsername + " has been Removed");
        saveUserChanges("Removed a Staff", idOfCurrentUser);
        hasChanges = true;

    }

    static void promoteStaffToAdmin(int index) {
        boolean validUserIndex = index != -1 && index < userCount;

        if (!validUserIndex) {
            System.out.println("\nUser Not Found\n");
            return;
        }

        //cannot promote admins
        if (userRole[index] != null && userRole[index] == Role.ADMIN) {
            System.out.println("\nUser is Already an Admin\n");
            return;
        }

        showUserInfo(index);
        String staffUsername = username[index];

        if (!confirmAction("Promote " + staffUsername + " to Admin?")) {
            System.out.println("Cancelled Promoting Staff");
            return;
        }
        //change selected staff role into ADMIN
        userRole[index] = Role.ADMIN;

        //save promotion to record history
        System.out.println("You Promoted Staff " + staffUsername + " to Admin");
        saveUserChanges("Promoted a Staff", idOfCurrentUser);
        hasChanges = true;
    }

    static void showUserInfo(int index) {
        //check if first if user exists
        boolean validUserIndex = index != -1 && index < userCount;

        if (!validUserIndex) {
            System.out.println("\nUser Not Found\n");
            return;
        }
        //display basic information of selected user
        System.out.println();
        System.out.println("User ID: " + userID[index]);
        System.out.println("Username: " + username[index]);
        System.out.println("Age: " + userAge[index]);
        System.out.println("Role: " + userRole[index]);
        System.out.println();
    }

    static void viewAllUsers() {
        System.out.println("\n===Users in Pet Adoption System===\n");

        //show all registered users including encrypted passwords
        System.out.printf("%-10s%-20s%-10s%-25s%s%n", "User ID:", "Username:", "Age:", "Password:", "Role:");
        System.out.println("-----------------------------------------------------------------------------------");

        for (int i = 0; i < userCount; i++) {
            System.out.printf("%-10s", userID[i]);
            System.out.printf("%-20s", username[i]);
            System.out.printf("%-10s", userAge[i]);
            System.out.printf("%-25s", encrypt(password[i]));
            System.out.printf("%s%n", userRole[i]);
        }

        //allow admin to continue directly to staff management
        while (true) {
            if (!confirmAction("Manage staff?")) {
                return;
            }
            if (!manageStaff()) {
                return;
            }
        }
    }

    //===============================================================================
    //==========================PET INPUT VALIDATION METHODS=========================
    //===============================================================================
    static boolean validateInput(String input, String regex) {
        //generic regex validator reused across multiple input methods
        return input.matches(regex);
    }

    static boolean validatePetName(String petName) {
        return validateInput(petName, "[A-Za-z.,\\s]{3,}");
    }

    static boolean validatePetAge(int age) {
        //pet age must be greater than zero
        if (age <= 0) {
            System.out.println("\nInvalid: Age Cannot Be Negative or Zero\n");
            return false;
        } else if (!(age >= 1 && age <= 99)){
            System.out.println("\nAge Must Be Between 1 to 99\n");
            return false;
        }

        return true;
    }

    static boolean validatePetForAdoption(String id) {
        //pet can only be adopted if:
        //1. ID exists
        //2. pet status is AVAILABLE
        for (int i = 0; i < petCount; i++) {
            if (petID[i] != null && petID[i].equals(id) && isAvailable(i)) {
                return true;
            }
        }
        return false;
    }
    //==============================================================================
    //======================USER AND ADOPTER VALIDATION METHODS=====================
    //===============================================================================

    static boolean validateName(String firstName, String middleInitial, String lastName) {

        //validate first name, optional middle initial, and last name
        String nameRegex = "[A-Za-z][A-Za-z .'-]{1,49}";
        String middleInitialRegex = "[A-Za-z].";

        boolean validFirst = firstName.matches(nameRegex);
        boolean validMiddle = middleInitial.isEmpty() || middleInitial.matches(middleInitialRegex);
        boolean validLast = lastName.matches(nameRegex);

        return validFirst && validMiddle && validLast;
    }

    static boolean validateAdopterAddress(String street, String barangay, String city) {

        //validate each part of the address separately
        String streetRegex = "[A-Za-z0-9\\s.,#/-]{3,100}";
        String barangayRegex = "[A-Za-z0-9\\s.-]{2,50}";
        String cityRegex = "[A-Za-z\\s.'-]{2,60}";

        boolean validStreet = street != null && street.matches(streetRegex);
        boolean validBarangay = barangay != null && barangay.matches(barangayRegex);
        boolean validCity = city != null && city.matches(cityRegex);

        return validStreet && validBarangay && validCity;
    }

    static boolean validateAdopterContact(String contact) {
        //Philippine mobile number validation
        //must start with 09 and contain 11 digits
        return validateInput(contact, "09\\d{9}");
    }

    static boolean validateUserAge(double age) {

        if (age <= 0) {
            System.out.println("\nAge Must not be Negative or Zero\n");
            return false;

            //user age must only be between 18 and 60
        } else if (age < 18 || age > 60) {
            System.out.println("\nAge Must be Between 18 and 60\n");
            return false;

        } else if (age % 1 == 0) {
            return true;

        } else {
            System.out.println("\nInvalid Age Format\n");
            return false;
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
        //returns true if pet status is AVAILABLE
        return petStatus[index] == Status.AVAILABLE;
    }

    static int countPetByStatus(Status status) {
        int count = 0;

        //counts pets depending on their adoption status
        //reusable counting method for AVAILABLE and ADOPTED pets
        for (int i = 0; i < petCount; i++) {
            if (petStatus[i] != null && petStatus[i] == status) {
                count++;
            }
        }
        return count;
    }

    static String prompt(String message) {
        //display message and return trimmed user input
        System.out.print(message + ": ");
        return input.nextLine().trim();
    }

    static String requiredPrompt(String message) {
        //keep asking until user enters a non-empty value
        while (true) {
            System.out.print(message + ": ");
            String str = input.nextLine().trim();

            if (str.isEmpty()) {
                System.out.println("\nThis Field is Required\n");
                continue;
            }
            return str;
        }
    }

    static boolean confirmAction(String message) {
        //only accepts "y" or "n" responses
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
           //stop loop immediately once match is found
            if (array[i] != null && array[i].equals(id)) {
                index = i;
                break;
            }
        }
        //returns index or -1 if ID is not found
        return index;
    }

    static String getCurrentDate() {
        LocalDateTime now = LocalDateTime.now();

        //format current date and time into readable text
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String date = now.format(formatter);
        return date;
    }

    static boolean addLineToFile(String filePath, String text) {

        //FileWriter(filePath, true) enables append mode
        //append new text to file instead of overwriting old contents
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {

            //return true if writing succeeds successfully
            writer.write(text + "\n");
            return true;

        } catch (IOException e) {
            return false;
        }
    }
}
