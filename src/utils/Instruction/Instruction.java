package utils.Instruction;

public class Instruction {
    public int lineNumber = 0;
    public String line = "";

    public String memoryLocation = "";
    public String opCode = "";

    public Mnemonic mnemonic = new Mnemonic("", "");

    public String[] segments;
    public String[] operands;

    public boolean isComment = false;
    public boolean isStartEnd = false;
    public boolean isDirective = false;


    public String standardMemoryLocationFormat() {
        StringBuilder stringBuilder = new StringBuilder(memoryLocation);
        while (stringBuilder.length() < 6) stringBuilder.insert(0, "0");

        if (stringBuilder.toString().toUpperCase().startsWith("A") ||
                stringBuilder.toString().toUpperCase().startsWith("B") ||
                stringBuilder.toString().toUpperCase().startsWith("C") ||
                stringBuilder.toString().toUpperCase().startsWith("D") ||
                stringBuilder.toString().toUpperCase().startsWith("E") ||
                stringBuilder.toString().toUpperCase().startsWith("F")) {

            stringBuilder.insert(0, "0");
        }

        return stringBuilder.toString();
    }
}
