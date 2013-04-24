package ca2;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class StudentRandomFile {
       
    private RandomAccessFile studentsFile = null;
    //Code , Index
    private HashMap<Integer, Integer> studentCodes = null;
    
    //Actual length allowed for those strings.
    public static final int F_NAME_LENGTH = 20;
    public static final int S_NAME_LENGTH = 20;
    
    public final boolean AGE_TYPE = false;
    public final boolean GPA_TYPE = true;
    
    //Size in bytes for all the fields
    private final int F_NAME_SIZE = F_NAME_LENGTH + 2;      // 2 + x chars allowed.
    private final int S_NAME_SIZE = S_NAME_LENGTH + 2;      // 2 + x chars allowed.
    private final int ID_SIZE = 4;                          // Int
    private final int SEX_SIZE = 1;                         // Boolean
    private final int CURRENT_GPA_SIZE = 8;                 // Double
    private final int AGE_SIZE = 4;                         // Int
    
    private final int RECORD_SIZE =  F_NAME_SIZE
                                   + S_NAME_SIZE
                                   + ID_SIZE
                                   + SEX_SIZE
                                   + CURRENT_GPA_SIZE
                                   + AGE_SIZE;
    


    
    public StudentRandomFile(String fileName) {
        try
        {
            studentsFile = new RandomAccessFile(fileName, "rw");
            studentCodes = this.getCodes();
        }
        catch(FileNotFoundException fnfe)
        {
            System.out.println("FileNotFoundException in StudentRandomFile::Constructor");
        }
    }
    
    public void finalize()
    {
        try
        {
            if(studentsFile != null) {
                studentsFile.close();
            }
        }
        catch(IOException ioe)
        {
            System.out.println("FileNotFoundException in StudentRandomFile::finalize()");
        }   
    }
    
    //**********************
    //  Private  ::  Reading Students
    //**********************
    
    private int getRecordCount() throws IOException
    {
        int recordCount = (int) studentsFile.length() / RECORD_SIZE;
        return recordCount;
    }
    
    private int getRecordNumber(int studentCode)
    {
        for(int i = 0; i<studentCodes.size();i++)
        {
            int code = studentCodes.get(i);
            if(studentCode == code)
            {
                return i;
            }
        }
        return -1;
    }
    
    private Student getRecord(int recordNumber) throws IOException
    {
        if(recordNumber >= 0 && recordNumber < this.getRecordCount())
        {
            studentsFile.seek(recordNumber * RECORD_SIZE);

            int id = studentsFile.readInt();
            String fName = IOStringUtils.readFixedString(studentsFile, F_NAME_SIZE);
            String sName = IOStringUtils.readFixedString(studentsFile, S_NAME_SIZE);
            int age = studentsFile.readInt();
            boolean sex = studentsFile.readBoolean();
            double gpa = studentsFile.readDouble();


            Student student = new Student(id, fName, sName, age, sex, gpa);

            return student;
        }
        else
        {
            return null;
        }
    }
    
    private HashMap<Integer, Integer> getCodes()
    {
        try
        {
            HashMap<Integer, Integer> codes = new HashMap<Integer, Integer>();
            for(int i = 0;i < this.getRecordCount(); i++)
            {
                studentsFile.seek(i * RECORD_SIZE);
                codes.put(studentsFile.readInt(), i);
            }
            return codes;
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
            return null;
        }
    }
    
    
    //**********************
    //  Public  ::  Reading Students
    //**********************
    
    
    
    public ArrayList<Student> getStudents()
    {
        ArrayList<Student> students = new ArrayList<Student>();
        
        try
        {
            for(int i = 0; i < getRecordCount(); i++)
            {
                Student student = this.getRecord(i);
                students.add(student);
            }
            return students;
        }
        catch(IOException ioe)
        {
            //report error
            return null;
        }
    }
    
    public Student getStudent(int studentCode)
    {
        try
        {
            int recordNumber = this.getRecordNumber(studentCode);
            Student student = this.getRecord(recordNumber);
            return student;
        }
        catch(IOException ioe)
        {
            //ERROR
            return null;
        }
    }
    
    public ArrayList<Student> searchByRange(int hi, int low, boolean type)
    {
        ArrayList<Student> students = getStudents();
        
        for(int i = 0; i < students.size(); i++)
        {
            if(type == AGE_TYPE)
            {
                if(students.get(i).getAge() > hi || students.get(i).getAge() < low)
                    students.remove(i);
            }
            else //GPA_TYPE
            {
                if(students.get(i).getCurrentGPA() > hi || students.get(i).getCurrentGPA() < low)
                    students.remove(i);
            }
            //ID - 10000000001
        }
        return students;
    }
    
    public ArrayList<Student> searchByRegex(String regex)
    {
        ArrayList<Student> students = getStudents();
        
        for(int i = 0; i < students.size(); i++)
        {
            if(!Pattern.matches(regex, students.get(i).getFirstName()))
                students.remove(i);
            else if(!Pattern.matches(regex, students.get(i).getLastName()))
                students.remove(i);
        }
        return students;
    }
    
    //**********************
    //  Private  ::  Writing Students
    //**********************
    
    private boolean writeStudent(Student student, int recordNumber)
    {
        try 
        {    
            studentsFile.seek(recordNumber * RECORD_SIZE);
            studentsFile.write(student.getID());
            IOStringUtils.writeFixedString(studentsFile,F_NAME_LENGTH, student.getFirstName());
            IOStringUtils.writeFixedString(studentsFile,S_NAME_LENGTH, student.getLastName());
            studentsFile.writeInt(student.getAge());
            studentsFile.writeBoolean(student.isSex());
            studentsFile.writeDouble(student.getCurrentGPA());
            return true;
        } catch (IOException ex) {
            Logger.getLogger(StudentRandomFile.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
    }
    
    //**********************
    //  Public  ::  Writing Students
    //**********************
    
    public boolean addStudent(Student student)
    {
        try {
            int recordNumber = (int)studentsFile.length() / RECORD_SIZE + 1;
            studentCodes.put(student.getID(), recordNumber);
            return this.writeStudent(student, recordNumber);       
        } catch (IOException ex) {
            Logger.getLogger(StudentRandomFile.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    //USED FOR EDITING
    public boolean updateStudent(Student student)
    {
        int recordNumber = getRecordNumber(student.getID());
        if(recordNumber != -1)
        {
            return this.writeStudent(student, recordNumber);
        }
        else
            return false;
        
    }
    
    public boolean deleteStudent(Student student)
    {
        try
        {
            if(getRecordNumber(student.getID()) != -1)
            {
                int deletionIndex = student.getID();
                Student lastStudent = getRecord((int)studentsFile.length()/ RECORD_SIZE - RECORD_SIZE); //Last item in the file NOT SURE IF RIGHT

                //Remove last from hash map
                studentCodes.remove(lastStudent.getID());
                //add with same value as the one to be deleted
                studentCodes.put(lastStudent.getID(), getRecordNumber(student.getID()));
                //Remove k,v to be deleted.
                studentCodes.remove(student.getID());

                //Update student, will get written into the space you want removed
                updateStudent(lastStudent);
                //set the file to be one file shorter, removing the duplicate.
                studentsFile.setLength(studentsFile.length() - RECORD_SIZE);

                return true;
            }
            else
                return false;
        }
        catch(IOException ioe)
        {
            //report error
            return false;
        }
    }
    
    public boolean deleteAll()
    {
        try {
            studentsFile.setLength(0);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(StudentRandomFile.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
}