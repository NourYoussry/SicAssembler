package utils;

import core.LookupTables;
import core.Settings;
import core.validators.ErrorController;
import utils.Instruction.Instruction;
import utils.Instruction.MnemonicFormat;
import utils.InstructionsEncoders.FormatFOUREncoder;
import utils.InstructionsEncoders.FormatTHREEEncoder;
import utils.InstructionsEncoders.FormatTWOEncoder;
import utils.errors.ErrorType;

public abstract class InstructionIdentifier {
    private static int PC = -1;
    public static int PASS = 1;

    public static Instruction identify(Instruction instruction) {
        // Trim Segments
        for (int i = 0; i < instruction.segments.length; i++)
            instruction.segments[i] = instruction.segments[i].trim();

        if (PASS == 1) {
            return PassONEIdentification(instruction);
        } else {
            return PassTWOIdentification(instruction);
        }
    }

    private static Instruction PassTWOIdentification(Instruction instruction) {
        if (instruction.segments[1].equals("EQU")) {
            diagnoseEQU(instruction);
            return instruction;
        }

        if (!instruction.isDirective && !instruction.isStartEnd) {
            if (instruction.mnemonic.format == MnemonicFormat.TWO)
                instruction = FormatTWOEncoder.encode(instruction);

            /*else if (instruction.mnemonic.format == MnemonicFormat.THREE)
                instruction = FormatTHREEEncoder.encode(instruction);*/

            else if (instruction.mnemonic.format == MnemonicFormat.FOUR)
                instruction = FormatFOUREncoder.encode(instruction);
        }

        return instruction;
    }

    private static Instruction PassONEIdentification(Instruction instruction) {
        instruction = FormatIdentifier.identify(instruction);

        assert instruction != null;

        instruction = diagnoseLabel(instruction);
        instruction = diagnoseLength(instruction);

        return instruction;
    }

    private static Instruction diagnoseLabel(Instruction instruction) {
        String label = instruction.segments[0];

        if (label.equals("")) return instruction;

        if (instruction.segments[1].equals("START")) {
            LookupTables.symbolTable.put(label, instruction.segments[2]);
            return instruction;
        }

        LookupTables.symbolTable.put(label, Integer.toHexString(PC));
        return instruction;
    }

    private static Instruction diagnoseLength(Instruction instruction) {
        int length = 0;

        if (instruction.isStartEnd)
            instruction = diagnoseStartEnd(instruction);

        else if (instruction.isDirective) {
            switch (instruction.segments[1]) {
                case "BYTE":
                    length = diagnoseBYTE(instruction.segments[2]);
                    Settings.programLength += length;
                    instruction.memoryLocation = Integer.toHexString(PC);
                    break;
                case "WORD":
                    length = diagnoseWORD(instruction.segments[2]);
                    Settings.programLength += length;
                    instruction.memoryLocation = Integer.toHexString(PC);
                    break;
                case "RESB":
                    length = Integer.parseInt(instruction.segments[2]);
                    Settings.programLength += length;
                    instruction.memoryLocation = Integer.toHexString(PC);
                    break;
                case "RESW":
                    length = 3 * Integer.parseInt(instruction.segments[2]);
                    Settings.programLength += length;
                    instruction.memoryLocation = Integer.toHexString(PC);
                    break;
                case "ORG":
                    instruction.memoryLocation = Integer.toHexString(PC);
                    PC = Integer.parseInt(instruction.segments[2]);
                    break;
                case "EQU":
                    instruction.memoryLocation = Integer.toHexString(PC);
                    break;
            }

            PC += length;
        } else {
            if (instruction.mnemonic.format == MnemonicFormat.TWO) length = 2;
            if (instruction.mnemonic.format == MnemonicFormat.THREE) length = 3;
            if (instruction.mnemonic.format == MnemonicFormat.FOUR) length = 4;

            Settings.programLength += length;
            instruction.memoryLocation = Integer.toHexString(PC);
            PC += length;
        }

        return instruction;
    }

    private static Instruction diagnoseStartEnd(Instruction instruction) {
        if (instruction.segments[1].equals("START")) {
            Settings.startLabel = instruction.segments[0].trim();
            Settings.startOperand = instruction.segments[2].trim();

            PC = Integer.parseInt(instruction.segments[2], 16);

            instruction.memoryLocation = Integer.toHexString(
                    Integer.parseInt(instruction.segments[2], 16)
            );
        } else instruction.memoryLocation = Integer.toHexString(PC);

        return instruction;
    }

    private static int diagnoseBYTE(String operand) {
        int length = 0;

        if (operand.startsWith("C")) {
            String data = operand.substring(2, operand.length() - 1);
            length = data.length();
        } else if (operand.startsWith("X")) {
            String data = operand.substring(2, operand.length() - 1);
            length = data.length() / 2;
        }

        return length;
    }

    private static int diagnoseWORD(String operand) {
        int length = 1;

        if (operand.contains(",")) {
            int i = 0, CommaCount = 0;

            while (i < operand.length()) {
                if (operand.charAt(i) == ',') CommaCount++;

                i++;
            }

            length = CommaCount + 1;
        }

        return 3 * length;
    }

    private static void diagnoseEQU(Instruction instruction) {

        boolean isHex = instruction.segments[2].matches("^[0-9a-fA-F]+$");

        if (isHex) {
            LookupTables.symbolTable.put(instruction.segments[0], instruction.segments[2]);
            return;
        }

        String memoryLocation = LookupTables.symbolTable.get(instruction.segments[2]);
        LookupTables.symbolTable.put(instruction.segments[0], memoryLocation);

    }

    private static void diagnoseLiteral(String operand){
        String address = "";
        StringBuilder duplicate = new StringBuilder();
        if(operand.startsWith("=c") ||
                operand.startsWith("=C") ||
                operand.startsWith("=x") ||
                operand.startsWith("=X") ||
                operand.startsWith("=w") ||
                operand.startsWith("=W")){
            duplicate.append("=X'");
            duplicate.append(literalToHex(operand));
            duplicate.append("'");
            if(!LookupTables.literalTable.containsKey(operand ) && !LookupTables.literalTable.containsKey(duplicate)){
                LookupTables.literalTable.put(operand, address);
            }
        }
    }
    private static int calculateLiteralLength(String literal){
        //case : (F20) = 1 byte or 2 bytes ?
        String hexa = literalToHex(literal);
        return(hexa.length() / 2);

    }
    private static String literalToHex (String literal){

        if(literal.startsWith("=c") || literal.startsWith("=C")) {
            StringBuilder builder = new StringBuilder();
            literal = literal.substring(3, literal.length()-1);
            char[] characters = literal.toCharArray();
            for (char c : characters) {
                int i = (int) c;
                builder.append(Integer.toHexString(i).toUpperCase());
            }
            return builder.toString();
        }

        else if(literal.startsWith("=w") || literal.startsWith("=W")){
            literal = literal.substring(3,literal.length()-1);
            int decimal =  (Integer.parseInt(literal));
            return (Integer.toHexString(decimal));
        }
        return (literal.substring(3, literal.length()-1));
    }

    private static boolean isDuplicateLiteral(String literal){
        literal = literalToHex(literal);
        //search litTab for =x'literal'
        //if found return true;
        return false;

    }
}
