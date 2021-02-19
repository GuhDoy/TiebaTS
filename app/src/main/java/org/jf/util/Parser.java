package org.jf.util;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.jf.dexlib.*;
import org.jf.dexlib.Code.*;
import org.jf.dexlib.Code.Format.*;
import org.jf.dexlib.CodeItem.*;
import org.jf.dexlib.Util.*;
import org.jf.dexlib.Util.SparseIntArray;
import org.jf.dexlib.Util.Utf8Utils;

import org.jf.util.IndentingWriter2;
public class Parser{
    public static final String hex_literal="([+,-])?0[x,X]([0-9,a-f,A-F])+";
    public static final Pattern pRegister=Pattern.compile("v\\d+");
    public static final Pattern pInt=Pattern.compile("\\s"+hex_literal+"|\\s([+,-])?\\d+");
    public static final Pattern pLong=Pattern.compile("\\s"+hex_literal+"([l,L])?|\\s([+,-])?\\d+([l,L])?");
    public static final Pattern pField=Pattern.compile("\\s|:|->");
    public static final Pattern pMethod=Pattern.compile("\\s|\\(|\\)|->");
    public static final String LABEL="label_";

    public static final String START="start : ";
    public static final String END="end : ";
    public static final String HANDLER="handler : ";
    public static final String ALL="all";

    public static final String CATCH=".catch ";
    public static final String ENDCATCH=".end catch";

    public final CodeItem code;
    public static int outWords;

    public Parser(CodeItem code){
        this.code=code;
    }

	
	
	
	
	
	
	public void dumpJava(IndentingWriter2 writer)throws IOException{
        if(code==null){
            return;
        }
        Instruction[] instructions=code.instructions;
        //cal offset label
        int currentCodeAddress=0;

        int[] labels=new int[instructions.length];
        Arrays.fill(labels,-1);

        SparseIntArray switchKey=new SparseIntArray(1);

        final HashMap<Integer,Integer> switchOpcodeOffsets=new HashMap<Integer,Integer>();
        for(int i=0,len=instructions.length;i<len;i++){
            Instruction instruction=instructions[i];
            switch(instruction.getFormat()){
                case Format10t:
                case Format20t:
                case Format21t:
                case Format22t:
                case Format30t:
                case Format31t:
                    {
                        OffsetInstruction off=(OffsetInstruction)instruction;
                        labels[i]=currentCodeAddress+off.getTargetAddressOffset();
                        // System.out.println(off.getTargetAddressOffset()+""); 
                        switch(instruction.opcode){
                            case PACKED_SWITCH:
                            case SPARSE_SWITCH:
                                switchOpcodeOffsets.put(labels[i],currentCodeAddress);
                                break;
                        }
                        break;
                    }
                case PackedSwitchData:
                case SparseSwitchData:
                    {
                        MultiOffsetInstruction mul=(MultiOffsetInstruction)instruction;
                        int switch_off=switchOpcodeOffsets.get(currentCodeAddress);
                        for(int target:mul.getTargets()){
                            switchKey.put(switch_off+target,0);
                        }
                        break;
                    }
            }
            currentCodeAddress+=instruction.getSize(currentCodeAddress);
        }


        //sort label offsets

        Arrays.sort(labels);


        int[] switchLabels=new int[]{-1};
        if(switchKey.size()>0)
            switchLabels=switchKey.keys();
        Arrays.sort(switchLabels);

        //try catch
        int[] tryLabels=tryItemLabels();
        Arrays.sort(tryLabels);


        /*
		 for(int tes: tryLabels){
		 System.out.println(""+tes);
		 }
		 */


        //dump instruction

        currentCodeAddress=0;
        for(int i=0,len=instructions.length;i<len;i++){
            Instruction instruction=instructions[i];

            //write Offset Instruction labels
            if(Arrays.binarySearch(labels,currentCodeAddress) >=0){
                writer.write(LABEL);
                writer.write(String.valueOf(currentCodeAddress));
				///     System.out.println(""+currentCodeAddress);
                writer.write(':');
                writer.write('\n');
            }else if(Arrays.binarySearch(tryLabels,currentCodeAddress) >=0){
                writer.write(LABEL);
                writer.write(String.valueOf(currentCodeAddress));
                writer.write(':');
                writer.write('\n');
            }

            //write sparse/packed switch labels
            if(Arrays.binarySearch(switchLabels,currentCodeAddress) >=0){
                writer.write("switch_");
                writer.printIntAsDec(currentCodeAddress);
                writer.write(':');
                writer.write('\n');
            }

            switch(instruction.getFormat()){
                case Format10x:
                    if(instruction.opcode==Opcode.NOP){
                        break;
                    }
                    writer.write(instruction.opcode.name);
                    writer.write('\n');
                    break;
                case Format10t:
                    Instruction10t ins10x=(Instruction10t)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writeLabel(writer,currentCodeAddress+ins10x.getTargetAddressOffset());
                    writer.write('\n');
                    break;
                case Format11x:
                    Instruction11x ins11x=(Instruction11x)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins11x.getRegisterA());
                    writer.write('\n');
                    break;
                case Format11n:
                    Instruction11n ins11n=(Instruction11n)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins11n.getRegisterA());
                    writer.write(' ');
                    writer.write(String.valueOf(ins11n.getLiteral()));
                    writer.write('\n');
                    break;
                case Format12x:
                    Instruction12x ins12x=(Instruction12x)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins12x.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins12x.getRegisterB());
                    writer.write('\n');
                    break;
                case Format20t:
                case Format30t:
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writeLabel(writer,currentCodeAddress+((OffsetInstruction)instruction).getTargetAddressOffset());
                    writer.write('\n');
                    break;
                case Format21t:
                    Instruction21t ins21t=(Instruction21t)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins21t.getRegisterA());
                    writer.write(' ');
                    writeLabel(writer,currentCodeAddress+ins21t.getTargetAddressOffset());
                    writer.write('\n');
                    break;
                case Format21c:
                case Format31c:
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(((SingleRegisterInstruction)instruction).getRegisterA());
                    writer.write(' ');
                    writeToReferencedItem(writer,instruction);
                    writer.write('\n');
                    break;
                case Format21h:
                case Format21s:
                case Format31i:
                case Format51l:
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(((SingleRegisterInstruction)instruction).getRegisterA());
                    writer.write(' ');
                    LongRenderer2.writeSignedIntOrLongTo(writer,((LiteralInstruction)instruction).getLiteral());
                    writer.write('\n');
                    break;
                case Format22x:
                    Instruction22x ins22x=(Instruction22x)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22x.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22x.getRegisterB());
                    writer.write('\n');
                    break;
                case Format22t:
                    Instruction22t ins22t=(Instruction22t)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22t.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22t.getRegisterB());
                    writer.write(' ');
                    writeLabel(writer,currentCodeAddress+ins22t.getTargetAddressOffset());
                    writer.write('\n');
                    break;
                case Format22b:
                    Instruction22b ins22b=(Instruction22b)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22b.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22b.getRegisterB());
                    writer.write(' ');
                    writer.write(String.valueOf(ins22b.getLiteral()));
                    writer.write('\n');
                    break;
                case Format22c:
                    Instruction22c ins22c=(Instruction22c)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22c.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22c.getRegisterB());
                    writer.write(' ');
                    writeToReferencedItem(writer,ins22c);
                    writer.write('\n');
                    break;
                case Format22s:
                    Instruction22s ins22s=(Instruction22s)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22s.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22s.getRegisterB());
                    writer.write(' ');
                    writer.write(String.valueOf(ins22s.getLiteral()));
                    writer.write('\n');
                    break;
                case Format23x:
                    Instruction23x ins23x=(Instruction23x)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins23x.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins23x.getRegisterB());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins23x.getRegisterC());
                    writer.write('\n');
                    break;
                case Format31t:
                    Instruction31t ins31t=(Instruction31t)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins31t.getRegisterA());
                    writer.write(' ');
                    writeLabel(writer,currentCodeAddress+ins31t.getTargetAddressOffset());
                    writer.write('\n');
                    break;
                case Format32x:
                    Instruction32x ins32x=(Instruction32x)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins32x.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins32x.getRegisterB());
                    writer.write('\n');
                    break;
                case Format35c:
                case Format35s:
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writeInvokeRegister(writer,instruction); 
                    writer.write(' ');
                    writeToReferencedItem(writer,instruction);
                    writer.write('\n');
                    break;
                case Format3rc:
                    Instruction3rc ins3rc=(Instruction3rc)instruction;
                    writer.write(instruction.opcode.name);
                    int rc=ins3rc.getRegCount();
                    int startReg=ins3rc.getStartRegister();
                    writer.write(' ');
                    writer.write('{');
                    writer.write('v');
                    writer.printIntAsDec(startReg);
                    writer.write("..");

                    int r=0;
                    for(;r<rc-1;r++);
                    writer.write('v');
                    writer.printIntAsDec(ins3rc.getStartRegister()+r);
                    writer.write('}');
                    writer.write(' ');
                    writeToReferencedItem(writer,ins3rc);
                    writer.write('\n');
                    break;
                case PackedSwitchData:
                    {
                        PackedSwitchDataPseudoInstruction pswitch=(PackedSwitchDataPseudoInstruction)instruction;
                        writer.write(".pswitch_data ");
                        int first=pswitch.getFirstKey();
                        writer.printIntAsDec(first);
                        writer.write('\n');
                        writer.indent(4);
                        int key=first;
                        int switch_off=switchOpcodeOffsets.get(currentCodeAddress);
                        for(int target:pswitch.getTargets()){
                            writeKeyAndTarget(writer,key++,switch_off+target);
                            writer.write('\n');
                        }
                        writer.deindent(4);
                        writer.write(".end pswitch_data");
                        writer.write('\n');
                        break;

                    }
                case SparseSwitchData:
                    {
                        SparseSwitchDataPseudoInstruction sswitch=(SparseSwitchDataPseudoInstruction)instruction;
                        writer.write(".sswitch_data");
                        writer.write('\n');
                        int switch_off=switchOpcodeOffsets.get(currentCodeAddress);
                        Iterator<SparseSwitchDataPseudoInstruction.SparseSwitchTarget> iterator=sswitch.iterateKeysAndTargets();
                        writer.indent(4);
                        while(iterator.hasNext()){
                            SparseSwitchDataPseudoInstruction.SparseSwitchTarget keyAndTarget=iterator.next();
                            writeKeyAndTarget(
								writer,keyAndTarget.key,switch_off+keyAndTarget.targetAddressOffset);
                            writer.write('\n');
                        }
                        writer.deindent(4);
                        writer.write(".end sswitch_data");
                        writer.write('\n');
                        break;

                    }
                case ArrayData:
                    {
                        ArrayDataPseudoInstruction arrayData=(ArrayDataPseudoInstruction)instruction;
                        writer.write(".array_data");
                        writer.write(' ');
                        int elementCount=arrayData.getElementCount();
                        int elementWidth=arrayData.getElementWidth();
                        writer.printIntAsDec(elementWidth);
                        writer.write('\n');
                        writer.indent(4);

                        Iterator<ArrayDataPseudoInstruction.ArrayElement> elements=arrayData.getElements();
                        while(elements.hasNext()){
                            ArrayDataPseudoInstruction.ArrayElement element=elements.next();
                            for(int width=element.bufferIndex;width<element.elementWidth+element.bufferIndex;width++){

                                ByteRenderer2.writeUnsignedTo(writer,element.buffer[width]);
                                writer.write(' ');
                            }
                            writer.write('\n');
                        }
                        writer.deindent(4);
                        writer.write(".end array_data");
                        writer.write('\n');
                        break;

                    }


            }
            currentCodeAddress+=instruction.getSize(currentCodeAddress);
			//	writer.write('\n');
        }

        //if label at end of instructions
        {
            if(Arrays.binarySearch(labels,currentCodeAddress) >=0){
                writer.write(LABEL);
                writer.write(String.valueOf(currentCodeAddress));
                //System.out.println(""+currentCodeAddress);
                writer.write(':');
                writer.write('\n');
            }else if(Arrays.binarySearch(tryLabels,currentCodeAddress) >=0){
                writer.write('\n');
                writer.write(LABEL);
                writer.write(String.valueOf(currentCodeAddress));
                writer.write(':');
                writer.write('\n');
            }

            //write sparse/packed switch labels
            if(Arrays.binarySearch(switchLabels,currentCodeAddress) >=0){
                writer.write("switch_");
                writer.write(String.valueOf(currentCodeAddress));
                writer.write(':');
                writer.write('\n');
            }
        }


        //write try catch

        writeTryItems(writer);
//                writer.flush();
    }
	
	
	
    public void dump(IndentingWriter2 writer)throws IOException{
        if(code==null){
            return;
        }
        Instruction[] instructions=code.instructions;
        //cal offset label
        int currentCodeAddress=0;

        int[] labels=new int[instructions.length];
        Arrays.fill(labels,-1);

        SparseIntArray switchKey=new SparseIntArray(1);

        final HashMap<Integer,Integer> switchOpcodeOffsets=new HashMap<Integer,Integer>();
        for(int i=0,len=instructions.length;i<len;i++){
            Instruction instruction=instructions[i];
            switch(instruction.getFormat()){
                case Format10t:
                case Format20t:
                case Format21t:
                case Format22t:
                case Format30t:
                case Format31t:
                    {
                        OffsetInstruction off=(OffsetInstruction)instruction;
                        labels[i]=currentCodeAddress+off.getTargetAddressOffset();
                        // System.out.println(off.getTargetAddressOffset()+""); 
                        switch(instruction.opcode){
                            case PACKED_SWITCH:
                            case SPARSE_SWITCH:
                                switchOpcodeOffsets.put(labels[i],currentCodeAddress);
                                break;
                        }
                        break;
                    }
                case PackedSwitchData:
                case SparseSwitchData:
                    {
                        MultiOffsetInstruction mul=(MultiOffsetInstruction)instruction;
                        int switch_off=switchOpcodeOffsets.get(currentCodeAddress);
                        for(int target:mul.getTargets()){
                            switchKey.put(switch_off+target,0);
                        }
                        break;
                    }
            }
            currentCodeAddress+=instruction.getSize(currentCodeAddress);
        }


        //sort label offsets

        Arrays.sort(labels);


        int[] switchLabels=new int[]{-1};
        if(switchKey.size()>0)
            switchLabels=switchKey.keys();
        Arrays.sort(switchLabels);

        //try catch
        int[] tryLabels=tryItemLabels();
        Arrays.sort(tryLabels);


        /*
           for(int tes: tryLabels){
           System.out.println(""+tes);
           }
           */


        //dump instruction

        currentCodeAddress=0;
        for(int i=0,len=instructions.length;i<len;i++){
            Instruction instruction=instructions[i];

            //write Offset Instruction labels
            if(Arrays.binarySearch(labels,currentCodeAddress) >=0){
                writer.write(LABEL);
                writer.write(String.valueOf(currentCodeAddress));
           ///     System.out.println(""+currentCodeAddress);
                writer.write(':');
                writer.write('\n');
            }else if(Arrays.binarySearch(tryLabels,currentCodeAddress) >=0){
                writer.write(LABEL);
                writer.write(String.valueOf(currentCodeAddress));
                writer.write(':');
                writer.write('\n');
            }

            //write sparse/packed switch labels
            if(Arrays.binarySearch(switchLabels,currentCodeAddress) >=0){
                writer.write("switch_");
                writer.printIntAsDec(currentCodeAddress);
                writer.write(':');
                writer.write('\n');
            }

            switch(instruction.getFormat()){
                case Format10x:
                    if(instruction.opcode==Opcode.NOP){
                        break;
                    }
                    writer.write(instruction.opcode.name);
                    writer.write('\n');
                    break;
                case Format10t:
                    Instruction10t ins10x=(Instruction10t)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writeLabel(writer,currentCodeAddress+ins10x.getTargetAddressOffset());
                    writer.write('\n');
                    break;
                case Format11x:
                    Instruction11x ins11x=(Instruction11x)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins11x.getRegisterA());
                    writer.write('\n');
                    break;
                case Format11n:
                    Instruction11n ins11n=(Instruction11n)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins11n.getRegisterA());
                    writer.write(' ');
                    writer.write(String.valueOf(ins11n.getLiteral()));
                    writer.write('\n');
                    break;
                case Format12x:
                    Instruction12x ins12x=(Instruction12x)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins12x.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins12x.getRegisterB());
                    writer.write('\n');
                    break;
                case Format20t:
                case Format30t:
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writeLabel(writer,currentCodeAddress+((OffsetInstruction)instruction).getTargetAddressOffset());
                    writer.write('\n');
                    break;
                case Format21t:
                    Instruction21t ins21t=(Instruction21t)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins21t.getRegisterA());
                    writer.write(' ');
                    writeLabel(writer,currentCodeAddress+ins21t.getTargetAddressOffset());
                    writer.write('\n');
                    break;
                case Format21c:
                case Format31c:
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(((SingleRegisterInstruction)instruction).getRegisterA());
                    writer.write(' ');
                    writeToReferencedItem(writer,instruction);
                    writer.write('\n');
                    break;
                case Format21h:
                case Format21s:
                case Format31i:
                case Format51l:
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(((SingleRegisterInstruction)instruction).getRegisterA());
                    writer.write(' ');
                    LongRenderer2.writeSignedIntOrLongTo(writer,((LiteralInstruction)instruction).getLiteral());
                    writer.write('\n');
                    break;
                case Format22x:
                    Instruction22x ins22x=(Instruction22x)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22x.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22x.getRegisterB());
                    writer.write('\n');
                    break;
                case Format22t:
                    Instruction22t ins22t=(Instruction22t)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22t.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22t.getRegisterB());
                    writer.write(' ');
                    writeLabel(writer,currentCodeAddress+ins22t.getTargetAddressOffset());
                    writer.write('\n');
                    break;
                case Format22b:
                    Instruction22b ins22b=(Instruction22b)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22b.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22b.getRegisterB());
                    writer.write(' ');
                    writer.write(String.valueOf(ins22b.getLiteral()));
                    writer.write('\n');
                    break;
                case Format22c:
                    Instruction22c ins22c=(Instruction22c)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22c.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22c.getRegisterB());
                    writer.write(' ');
                    writeToReferencedItem(writer,ins22c);
                    writer.write('\n');
                    break;
                case Format22s:
                    Instruction22s ins22s=(Instruction22s)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22s.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins22s.getRegisterB());
                    writer.write(' ');
                    writer.write(String.valueOf(ins22s.getLiteral()));
                    writer.write('\n');
                    break;
                case Format23x:
                    Instruction23x ins23x=(Instruction23x)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins23x.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins23x.getRegisterB());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins23x.getRegisterC());
                    writer.write('\n');
                    break;
                case Format31t:
                    Instruction31t ins31t=(Instruction31t)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins31t.getRegisterA());
                    writer.write(' ');
                    writeLabel(writer,currentCodeAddress+ins31t.getTargetAddressOffset());
                    writer.write('\n');
                    break;
                case Format32x:
                    Instruction32x ins32x=(Instruction32x)instruction;
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins32x.getRegisterA());
                    writer.write(' ');
                    writer.write('v');
                    writer.printIntAsDec(ins32x.getRegisterB());
                    writer.write('\n');
                    break;
                case Format35c:
                case Format35s:
                    writer.write(instruction.opcode.name);
                    writer.write(' ');
                    writeInvokeRegister(writer,instruction); 
                    writer.write(' ');
                    writeToReferencedItem(writer,instruction);
                    writer.write('\n');
                    break;
                case Format3rc:
                    Instruction3rc ins3rc=(Instruction3rc)instruction;
                    writer.write(instruction.opcode.name);
                    int rc=ins3rc.getRegCount();
                    int startReg=ins3rc.getStartRegister();
                    writer.write(' ');
                    writer.write('{');
                    writer.write('v');
                    writer.printIntAsDec(startReg);
                    writer.write("..");

                    int r=0;
                    for(;r<rc-1;r++);
                    writer.write('v');
                    writer.printIntAsDec(ins3rc.getStartRegister()+r);
                    writer.write('}');
                    writer.write(' ');
                    writeToReferencedItem(writer,ins3rc);
                    writer.write('\n');
                    break;
                case PackedSwitchData:
                    {
                        PackedSwitchDataPseudoInstruction pswitch=(PackedSwitchDataPseudoInstruction)instruction;
                        writer.write(".pswitch_data ");
                        int first=pswitch.getFirstKey();
                        writer.printIntAsDec(first);
                        writer.write('\n');
                        writer.indent(4);
                        int key=first;
                        int switch_off=switchOpcodeOffsets.get(currentCodeAddress);
                        for(int target:pswitch.getTargets()){
                            writeKeyAndTarget(writer,key++,switch_off+target);
                            writer.write('\n');
                        }
                        writer.deindent(4);
                        writer.write(".end pswitch_data");
                        writer.write('\n');
                        break;

                    }
                case SparseSwitchData:
                    {
                        SparseSwitchDataPseudoInstruction sswitch=(SparseSwitchDataPseudoInstruction)instruction;
                        writer.write(".sswitch_data");
                        writer.write('\n');
                        int switch_off=switchOpcodeOffsets.get(currentCodeAddress);
                        Iterator<SparseSwitchDataPseudoInstruction.SparseSwitchTarget> iterator=sswitch.iterateKeysAndTargets();
                        writer.indent(4);
                        while(iterator.hasNext()){
                            SparseSwitchDataPseudoInstruction.SparseSwitchTarget keyAndTarget=iterator.next();
                            writeKeyAndTarget(
                                    writer,keyAndTarget.key,switch_off+keyAndTarget.targetAddressOffset);
                            writer.write('\n');
                        }
                        writer.deindent(4);
                        writer.write(".end sswitch_data");
                        writer.write('\n');
                        break;

                    }
                case ArrayData:
                    {
                        ArrayDataPseudoInstruction arrayData=(ArrayDataPseudoInstruction)instruction;
                        writer.write(".array_data");
                        writer.write(' ');
                        int elementCount=arrayData.getElementCount();
                        int elementWidth=arrayData.getElementWidth();
                        writer.printIntAsDec(elementWidth);
                        writer.write('\n');
                        writer.indent(4);

                        Iterator<ArrayDataPseudoInstruction.ArrayElement> elements=arrayData.getElements();
                        while(elements.hasNext()){
                            ArrayDataPseudoInstruction.ArrayElement element=elements.next();
                            for(int width=element.bufferIndex;width<element.elementWidth+element.bufferIndex;width++){

                                ByteRenderer2.writeUnsignedTo(writer,element.buffer[width]);
                                writer.write(' ');
                            }
                            writer.write('\n');
                        }
                        writer.deindent(4);
                        writer.write(".end array_data");
                        writer.write('\n');
                        break;

                    }


            }
            currentCodeAddress+=instruction.getSize(currentCodeAddress);
		//	writer.write('\n');
        }

        //if label at end of instructions
        {
            if(Arrays.binarySearch(labels,currentCodeAddress) >=0){
                writer.write(LABEL);
                writer.write(String.valueOf(currentCodeAddress));
                //System.out.println(""+currentCodeAddress);
                writer.write(':');
                writer.write('\n');
            }else if(Arrays.binarySearch(tryLabels,currentCodeAddress) >=0){
                writer.write('\n');
                writer.write(LABEL);
                writer.write(String.valueOf(currentCodeAddress));
                writer.write(':');
                writer.write('\n');
            }

            //write sparse/packed switch labels
            if(Arrays.binarySearch(switchLabels,currentCodeAddress) >=0){
                writer.write("switch_");
                writer.write(String.valueOf(currentCodeAddress));
                writer.write(':');
                writer.write('\n');
            }
        }


        //write try catch

        writeTryItems(writer);
//                writer.flush();
    }


    private static void writeInvokeRegister(IndentingWriter2 writer,Instruction instruction)throws IOException{
        FiveRegisterInstruction five = (FiveRegisterInstruction)instruction;
        final int regCount = five.getRegCount();

        writer.write('{');
        switch (regCount) {
            case 1:
                writer.write('v');
                writer.printIntAsDec(five.getRegisterD());
                break;
            case 2:
                writer.write('v');
                writer.printIntAsDec(five.getRegisterD());
                writer.write(",");
                writer.write('v');
                writer.printIntAsDec(five.getRegisterE());
                break;
            case 3:
                writer.write('v');
                writer.printIntAsDec(five.getRegisterD());
                writer.write(",");
                writer.write('v');
                writer.printIntAsDec(five.getRegisterE());
                writer.write(",");
                writer.write('v');
                writer.printIntAsDec(five.getRegisterF());
                break;
            case 4:
                writer.write('v');
                writer.printIntAsDec(five.getRegisterD());
                writer.write(",");
                writer.write('v');
                writer.printIntAsDec(five.getRegisterE());
                writer.write(",");
                writer.write('v');
                writer.printIntAsDec(five.getRegisterF());
                writer.write(",");
                writer.write('v');
                writer.printIntAsDec(five.getRegisterG());
                break;
            case 5:
                writer.write('v');
                writer.printIntAsDec(five.getRegisterD());
                writer.write(",");
                writer.write('v');
                writer.printIntAsDec(five.getRegisterE());
                writer.write(",");
                writer.write('v');
                writer.printIntAsDec(five.getRegisterF());
                writer.write(",");
                writer.write('v');
                writer.printIntAsDec(five.getRegisterG());
                writer.write(",");
                writer.write("v");
                writer.printIntAsDec(five.getRegisterA());
                break;
        }
        writer.write('}');
    }

    private static void writeLabel(IndentingWriter2 writer,int off)throws IOException{
        writer.write(':');
        writer.write(LABEL);
        writer.write(String.valueOf(off));
    }


    private static void writeKeyAndTarget(IndentingWriter2 writer,int key,int target)throws IOException{

        writer.write(String.valueOf(key));
        writer.write(" : switch_");
        writer.write(String.valueOf(target));
    }


    private static void writeToReferencedItem(IndentingWriter2 writer,Instruction instruction)throws IOException {
        InstructionWithReference ref=(InstructionWithReference)instruction;
        switch (instruction.opcode.referenceType) {
            case field:
                FieldIdItem field=(FieldIdItem)ref.getReferencedItem();
                writer.write(field.getFieldString());
                return;
            case method:
                MethodIdItem method=(MethodIdItem)ref.getReferencedItem();
                writer.write(method.getMethodString());
                return;
            case type:
                TypeIdItem type=(TypeIdItem)ref.getReferencedItem();
                writer.write(type.getTypeDescriptor());
                return;
            case string:
                StringIdItem string=(StringIdItem)ref.getReferencedItem();
                writer.write('\"');
                Utf8Utils.writeEscapedString(writer,string.getStringValue());
                writer.write('\"');
        }
    }


    public void parse(DexFile dexFile,String string)throws Exception{
        if(code==null){
            return;
        }
        {
            outWords=0;
        }
        String[] strings=string.split("\n");

        final HashMap<String,Integer> labelsMapOffsets=new HashMap<String,Integer>();
        final HashMap<String,Integer> switchOpcodeOffsets=new HashMap<String,Integer>(1);
        int currentCodeAddress=0;

        Opcode[] opcodes=new Opcode[strings.length];
        List<Instruction> instructions=new ArrayList<Instruction>(strings.length);
        List<Instruction> dataPseudoInstructions=new ArrayList<Instruction>(1);
        Pair<List<TryItem>,List<EncodedCatchHandler>> tryItems=null;
        for(int i=0,len=strings.length;i<len;i++){
            String content=strings[i].trim();
            // unused
            if(content.equals("")
                    ||content.startsWith("#")
                    ||content.startsWith("//")){
                opcodes[i]=null;
                continue;
                    }
            //labels
            if(content.endsWith(":")){
                labelsMapOffsets.put(content.substring(0,content.length()-1),currentCodeAddress);
                opcodes[i]=null;
                continue;
            }

            int index=content.indexOf(" ");
            String opcodeName="";
            if(index!=-1)
                opcodeName=content.substring(0,index);
            else
                opcodeName=content.trim();

            Opcode opcode=Opcode.getOpcodeByName(opcodeName);
            if(opcode ==null){
                //at last
                //packed switch parse
                if(opcodeName.equals(".pswitch_data")){
                    String switchOff=strings[i-1].trim();
                    int switchOpcodeOffset=switchOpcodeOffsets.get(switchOff.substring(0,switchOff.length()-1));
                    SparseIntArray keysAndTargets=new SparseIntArray();
                    while(true){
                        String nextKeyAndTarget=strings[++i].trim();
                                //System.out.println(nextKeyAndTarget);
                        if(nextKeyAndTarget.startsWith(".end")){
                            break;
                        }
                        String[] sp=nextKeyAndTarget.split(":");
                        if(sp.length != 2){
                            throw new IllegalArgumentException( "packed switch data error: "+nextKeyAndTarget);
                        }
                        try{
                            keysAndTargets.put(
                                    Integer.parseInt(sp[0].trim()),
                                    labelsMapOffsets.get(sp[1].trim())-switchOpcodeOffset);
                        }catch(Exception e){
                            throw new IllegalArgumentException( "the key is not int or label is not exists: "+nextKeyAndTarget);
                        }
                    }
                    int firstKey=keysAndTargets.keyAt(0);
                    int[] targetsArray=keysAndTargets.values();
                    PackedSwitchDataPseudoInstruction packedSwitch=new PackedSwitchDataPseudoInstruction(firstKey,targetsArray);
                    currentCodeAddress+=packedSwitch.getSize(currentCodeAddress);
                    dataPseudoInstructions.add(packedSwitch);

                    //sparse switch parse
                }else if(opcodeName.equals(".sswitch_data")){
                    String switchOff=strings[i-1].trim();
                    int switchOpcodeOffset=switchOpcodeOffsets.get(switchOff.substring(0,switchOff.length()-1));
                    SparseIntArray keysAndTargets=new SparseIntArray();
                    while(true){
                        String nextKeyAndTarget=strings[++i].trim();
                                 //System.out.println(nextKeyAndTarget);
                        if(nextKeyAndTarget.startsWith(".end")){
                            break;
                        }
                        String[] sp=nextKeyAndTarget.split(":");
                        if(sp.length != 2){
                            throw new IllegalArgumentException( "packed switch data error: "+nextKeyAndTarget+ " at "+i);
                        }
                        try{
                            keysAndTargets.put(
                                    Integer.parseInt(sp[0].trim()),
                                    labelsMapOffsets.get(sp[1].trim())-switchOpcodeOffset
                                    );
                        }catch(Exception e){
                            throw new IllegalArgumentException( "the key is not int or label is not exists: "+nextKeyAndTarget+" at "+ i);
                        }
                    }
                    int[] keysArray=keysAndTargets.keys();

                    int[] targetsArray=keysAndTargets.values();
                    SparseSwitchDataPseudoInstruction sparseSwitch=new SparseSwitchDataPseudoInstruction(keysArray,targetsArray);
                    dataPseudoInstructions.add(sparseSwitch);
                    currentCodeAddress+=sparseSwitch.getSize(currentCodeAddress);
                }else if(opcodeName.equals(".array_data")){
                    String[] sp=strings[i].trim().split(" ");
                    if(sp.length<2)
                        throw new IllegalArgumentException( "unknow element Width: "+strings[i]);
                    int elementWidth=LiteralTools2.parseInt(sp[1]);
                    ByteArrayOutputStream baos=new ByteArrayOutputStream();
                    while(true){
                        String encodeValue=strings[++i].trim();
                        if(encodeValue.startsWith(".end")){
                            break;
                        }
                        String[] encodeValues=encodeValue.split(" ");
                        if(encodeValues.length != elementWidth){
                            throw new IllegalArgumentException( "encodeValues width:"+encodeValues.length+" does not match : "+elementWidth+" at "+i);
                        }
                        for(String value :encodeValues){
                            baos.write(LiteralTools2.parseByte(value.trim()));
                        }
                    }
                    ArrayDataPseudoInstruction arrayData=new ArrayDataPseudoInstruction(elementWidth,baos.toByteArray());
                    dataPseudoInstructions.add(arrayData);
                    currentCodeAddress+=arrayData.getSize(currentCodeAddress);

                    //catchs at last
                }else if(opcodeName.equals(".catch")){
                    tryItems=parseCatchs(dexFile,strings,i,labelsMapOffsets);
                    break;
                }else{
                    throw new IllegalArgumentException( "unknow opcodeName: "+opcodeName+" at "+ i);
                }
            }else{
                switch(opcode){
                    case PACKED_SWITCH:
                    case SPARSE_SWITCH:
                        String[] sp=content.split(":");
                        if(sp.length != 2){
                            throw new IllegalArgumentException( " opcode offset error: "+content);
                        }
                        switchOpcodeOffsets.put(sp[1].trim(),currentCodeAddress);
                        break;
                }
                currentCodeAddress+=opcode.format.size/2;
            }
            opcodes[i]=opcode;
        }

        //assamble instructions 
        currentCodeAddress=0;
        for(int i=0,len=strings.length;i<len;i++){
            String str=strings[i].trim();
            Opcode opcode=opcodes[i];
            if(opcode!=null){
               // System.out.println(opcode);
               // System.out.println(opcode.format);

                switch(opcode.format){
                    case Format10x:
                        {
                            instructions.add(new Instruction10x(opcode));
                            break;
                        }
                    case Format10t:
                        {
                            Instruction10t ins=new Instruction10t(opcode,(byte)parseTarget(str,labelsMapOffsets,currentCodeAddress));

                            //      System.out.println(""+ins.getTargetAddressOffset()); 
                            instructions.add(ins);
                            break;
                        }
                    case Format11n:
                        {
                            byte regA=(byte)parseSingleRegister(str);
                            byte litB=(byte)parseInt(str);
                            Instruction11n ins=new Instruction11n(opcode,regA,litB);
                            instructions.add(ins);
                            //  System.out.println(""+ins.getLiteral()); 

                            break;
                        }
                    case Format11x:
                        {
                            short regA=(short)parseSingleRegister(str);
                            instructions.add(new Instruction11x(opcode,regA));
                            break;
                        }
                    case Format12x:
                        {

                            Matcher m=pRegister.matcher(str);
                            if(!m.find());
                            byte regA=(byte)Integer.parseInt(m.group().substring(1));
                            if(!m.find());
                            byte regB=(byte)Integer.parseInt(m.group().substring(1));
                            instructions.add(new Instruction12x(opcode,regA,regB));
                            break;
                        }
                    case Format20t:
                        {
                            short offset=(short)(parseTarget(str,labelsMapOffsets,currentCodeAddress)&0xFFFF);
                            Instruction20t ins=new Instruction20t(opcode,offset);

                            //    System.out.println(""+ins.getTargetAddressOffset()); 
                            instructions.add(ins);
                            break;
                        }
                    case Format21c:
                        {
                            short regA=(short)parseSingleRegister(str);
                            Item item=null;
                            switch(opcode.referenceType){
                                case field:
                                    try{
                                    item=parseField(dexFile,str);
                                    }catch(Exception e){
                                        throw new Exception("FieldIdItem error: "+str+"at "+i);
                                    }
                                    break;
                                case string:
                                    try{
                                    item=parseString(dexFile,str);
                                    }catch(Exception e){
                                        throw new Exception("String error: "+str+"at "+i);
                                    }
                                    break;
                                case type:
                                    try{
                                    item=parseType(dexFile,str);
                                    }catch(Exception e){
                                        throw new Exception("TypeIdItem error: "+str+"at "+i);
                                    }
                                    break;
                            }
                            instructions.add(new Instruction21c(opcode,regA,item));
                            break;
                        }
                    case Format21h:
                        {
                            short regA=(short)parseSingleRegister(str);
                            short litB=(short)(parseInt(str)&0xFFFF);
                            Instruction21h ins=new Instruction21h(opcode,regA,litB);
                            instructions.add(ins);
                            //      System.out.println(""+ins.getLiteral()); 
                            break;
                        }
                    case Format21s:
                        {
                            short regA=(short)parseSingleRegister(str);
                            short litB=(short)(parseInt(str)&0xFFFF);
                            Instruction21s ins=new Instruction21s(opcode,regA,litB);
                            instructions.add(ins);
                            //        System.out.println(""+ins.getLiteral()); 
                            break;
                        }
                    case Format21t:
                        {
                            short regA=(short)parseSingleRegister(str);
                            short offset=(short)(parseTarget(str,labelsMapOffsets,currentCodeAddress)&0xFFFF);
                            Instruction21t ins=new Instruction21t(opcode,regA,offset);
                            instructions.add(ins);
                            //          System.out.println(""+ins.getTargetAddressOffset()); 
                            break;
                        }
                    case Format22b:
                        {

                            Matcher m=pRegister.matcher(str);
                            if(!m.find());
                            short regA=(short)Integer.parseInt(m.group().substring(1));
                            if(!m.find());
                            short regB=(short)Integer.parseInt(m.group().substring(1));
                            byte litC=(byte)(parseInt(str)&0xFF);
                            instructions.add(new Instruction22b(opcode,regA,regB,litC));
                            break;
                        }
                    case Format22s:
                        {

                            Matcher m=pRegister.matcher(str);
                            if(!m.find());
                            byte regA=(byte)Integer.parseInt(m.group().substring(1));
                            if(!m.find());
                            byte regB=(byte)Integer.parseInt(m.group().substring(1));
                            short litC=(short)(parseInt(str)&0xFFFF);
                            instructions.add(new Instruction22s(opcode,regA,regB,litC));
                            break;
                        }
                    case Format22t:
                        {

                            Matcher m=pRegister.matcher(str);
                            if(!m.find());
                            byte regA=(byte)Integer.parseInt(m.group().substring(1));
                            if(!m.find());
                            byte regB=(byte)Integer.parseInt(m.group().substring(1));
                            short offset=(short)(parseTarget(str,labelsMapOffsets,currentCodeAddress)&0xFFFF);
                            instructions.add(new Instruction22t(opcode,regA,regB,offset));
                            break;
                        }
                    case Format22c:
                        {

                            Matcher m=pRegister.matcher(str);
                            if(!m.find());
                            byte regA=(byte)Integer.parseInt(m.group().substring(1));
                            if(!m.find());
                            byte regB=(byte)Integer.parseInt(m.group().substring(1));

                            Item item=null;
                            switch(opcode.referenceType){
                                case field:
                                    item=parseField(dexFile,str);
                                    break;
                                case type:
                                    item=parseType(dexFile,str);
                                    break;
                            }
                            instructions.add(new Instruction22c(opcode,regA,regB,item));
                            break;
                        }
                    case Format22x:
                        {

                            Matcher m=pRegister.matcher(str);
                            if(!m.find());
                            short regA=(short)(Integer.parseInt(m.group().substring(1))&0xFFFF);
                            if(!m.find());
                            int regB=Integer.parseInt(m.group().substring(1))&0xFFFF;
                            instructions.add(new Instruction22x(opcode,regA,regB));
                            break;
                        }
                    case Format23x:
                        {

                            Matcher m=pRegister.matcher(str);
                            if(!m.find());
                            short regA=(short)Integer.parseInt(m.group().substring(1));
                            if(!m.find());
                            short regB=(short)Integer.parseInt(m.group().substring(1));
                            if(!m.find());
                            short regC=(short)Integer.parseInt(m.group().substring(1));
                            instructions.add(new Instruction23x(opcode,regA,regB,regC));
                            break;
                        }
                    case Format30t:
                        {
                            Instruction30t ins=new Instruction30t(opcode,parseTarget(str,labelsMapOffsets,currentCodeAddress));

                            instructions.add(ins);
                            break;
                        }
                    case Format31c:
                        {
                            short regA=(short)parseSingleRegister(str);
                            Item item=parseString(dexFile,str);
                            instructions.add(new Instruction31c(opcode,regA,item));
                            break;
                        }
                    case Format31i:
                        {
                            byte regA=(byte)parseSingleRegister(str);
                            int litB=parseInt(str);
                            Instruction31i ins=new Instruction31i(opcode,regA,litB);
                            instructions.add(ins);
                            break;
                        }
                    case Format31t:
                        {
                            short regA=(short)parseSingleRegister(str);
                            int offset=parseTarget(str,labelsMapOffsets,currentCodeAddress);
                            Instruction31t ins=new Instruction31t(opcode,regA,offset);
                            instructions.add(ins);
                            //           System.out.println(""+ins.getTargetAddressOffset()); 
                            break;
                        }
                    case Format32x:
                        {

                            Matcher m=pRegister.matcher(str);
                            if(!m.find());
                            int regA=Integer.parseInt(m.group().substring(1));
                            if(!m.find());
                            int  regB=Integer.parseInt(m.group().substring(1));
                            instructions.add(new Instruction32x(opcode,regA,regB));
                            break;
                        }
                    case Format35c:
                        {
                            int[] regCount=new int[1];
                            byte[] regs=parseFiveRegister(str,regCount);
                            Item item=null;
                            switch(opcode.referenceType){
                                case method:
                                    item=parseMethod(dexFile,str);
                                    break;
                                case type:
                                    item=parseType(dexFile,str);
                                    break;
                            }
                            try{
                            instructions.add(new Instruction35c(opcode,regCount[0],regs[0],regs[1],regs[2],regs[3],regs[4],item));
                            }catch(Exception e){
                                throw new RuntimeException(str+"  "+item+"  "+regCount[0]);
                            }
                            break;
                        }
                    case Format35s:
                        break;
                    case Format3rc:
                        {
                            int[] regs=parseRangeRegister(str);

                            Item item=null;
                            switch(opcode.referenceType){
                                case method:
                                    item=parseMethod(dexFile,str);
                                    break;
                                case type:
                                    item=parseType(dexFile,str);
                                    break;
                            }
                            instructions.add(new Instruction3rc(opcode,(short)regs[0]/*register count*/,regs[1]/*start register*/,item));
                            break;
                        }
                    case Format51l:
                        {
                            short regA=(short)parseSingleRegister(str);
                            long litB=parseLong(str);
                            Instruction51l ins=new Instruction51l(opcode,regA,litB);
                            instructions.add(ins);
                            //            System.out.println(""+ins.getLiteral()); 
                            break;
                        }



                }
                currentCodeAddress+=opcode.format.size/2;
            }
        }
        //add Data Pseudo instructions
        instructions.addAll(dataPseudoInstructions);

        //update instructions
        Instruction[] update=new Instruction[instructions.size()];
        instructions.toArray(update);

        //try catch handler
        if(tryItems !=null){
            List<TryItem> first=tryItems.first;
            List<EncodedCatchHandler> second=tryItems.second;
            if(first.size()>0){
                TryItem[] tries=new TryItem[first.size()];
                EncodedCatchHandler[] encodedCatchHandlers=new EncodedCatchHandler[second.size()];
                first.toArray(tries);
                second.toArray(encodedCatchHandlers);

                code.tries=tries;
                code.encodedCatchHandlers=encodedCatchHandlers;
            }
        }else{
            code.tries=null;
            code.encodedCatchHandlers=null;
        }


        //set inWords and outWords
        ClassDataItem.EncodedMethod method=code.getParent();
        int parameterRegisterCount=method.method.getPrototype().getParameterRegisterCount();

        //not static
        if((method.accessFlags&AccessFlags.STATIC.getValue()) == 0){
            parameterRegisterCount++;
        }
        code.inWords=parameterRegisterCount;
        code.outWords=outWords;

        //update CodeItem
        code.updateCode(update);

    }


    private static byte[] parseFiveRegister(String s,int[] len){
        byte[] regs=new byte[5];
        Arrays.fill(regs,(byte)0);

        int j=s.indexOf('{');
        int k=s.indexOf('}');
        Matcher m=pRegister.matcher(s.substring(j+1,k));
        int i=0;
        while(m.find()&&i<5){
            int reg=Integer.parseInt(m.group().substring(1));
            regs[i++]=(byte)reg;
        }
        len[0]=i;
        outWords=Math.max(outWords,i);
        return regs;
    }


    private static int[] parseRangeRegister(String s){
        int[] regs=new int[2];//regs[0] reg count regs[1] start reg

        int j=s.indexOf('{');
        int k=s.indexOf('}');
        Matcher m=pRegister.matcher(s.substring(j+1,k));
        if(!m.find());
        regs[1]=Integer.parseInt(m.group().substring(1));
        int i=0;
        while(m.find()){
            i=Integer.parseInt(m.group().substring(1));
            //System.out.println("range "+i);
        }
        int count=i-regs[1]+1;//reg count
        regs[0]=count;
        outWords=Math.max(outWords,count);
           // System.out.println("count "+regs[0]);
        return regs;
    }



    private static FieldIdItem parseField(DexFile dexFile,String s){

        String[] strs=pField.split(s);
        // for(String a:strs)
        //   System.out.println(a.trim());
        int i=strs.length-1;
        if(i<2)
            throw new RuntimeException("FieldIdItem error: "+s);

        String classType=strs[i-2];
        String name=strs[i-1];
        String type=strs[i];
        //                System.out.println(classType+"  "+name+"   "+type);
        return FieldIdItem.internFieldIdItem(
                dexFile,
                TypeIdItem.internTypeIdItem(
                    dexFile,
                    classType),
                TypeIdItem.internTypeIdItem(
                    dexFile,
                    type),
                StringIdItem.internStringIdItem(
                    dexFile,
                    name)
                );
    }


    private static MethodIdItem parseMethod(DexFile dexFile,String s){

        String[] strs=pMethod.split(s);
        int i=strs.length-1;
        if(i<3)
            throw new RuntimeException("MethodIdItem error: "+s);
        String classType=strs[i-3];
        String name=strs[i-2];
        TypeListItem params=buildTypeList(dexFile,strs[i-1]);
        String returnType=strs[i];
        //    System.out.println(classType+"   "+name+"   "+strs[i-1]+"   "+returnType);
        ProtoIdItem proto=ProtoIdItem.internProtoIdItem(
                dexFile,
                TypeIdItem.internTypeIdItem(
                    dexFile,
                    returnType),
                params
                );
        return MethodIdItem.internMethodIdItem(
                dexFile,
                TypeIdItem.internTypeIdItem(
                    dexFile,
                    classType),
                proto,
                StringIdItem.internStringIdItem(
                    dexFile,
                    name)
                );
    }


    private static TypeIdItem parseType(DexFile dexFile,String s){
        s=s.trim();
        int i=s.lastIndexOf(" ");
        String type =s.substring(i+1);
        return TypeIdItem.internTypeIdItem(
                dexFile,
                type
                );
    }

    private static StringIdItem parseString(DexFile dexFile,String s){
        int i=s.indexOf("\"");
        int j=s.lastIndexOf("\"");
        return StringIdItem.internStringIdItem(
                dexFile,
                Utf8Utils.escapeSequence(s.substring(i+1,j))
                );
    }


    private static int parseInt(String s)throws Exception{
        Matcher m=pInt.matcher(s);
        if(!m.find())
            throw new Exception("int exception: "+s);
        return LiteralTools2.parseInt(m.group().trim());

    }

    private static int parseTarget(String s,HashMap<String,Integer> targetOffsets,int codeOffset){
        try{
            String[] str=s.split(":");
            int off=targetOffsets.get(str[1].trim())-codeOffset;
            //System.out.println("parse "+off);
            return off;
        }catch(Exception e){
            throw new IllegalArgumentException( "unfound label offset: "+s);
        }
    }

    private static long parseLong(String s)throws Exception{
        Matcher m=pLong.matcher(s);
        if(!m.find())
            throw new Exception("long exception: "+s);
        return LiteralTools2.parseLong(m.group().trim());

    }

    private static int parseSingleRegister(String s)throws Exception{

        Matcher m=pRegister.matcher(s);
        if(!m.find())
            throw new Exception("register exception: "+s);
        int reg= Integer.parseInt(m.group().substring(1));
        return reg;
    }



    public static TypeListItem buildTypeList(DexFile dexFile,String str){
        List<TypeIdItem> typeList=new ArrayList<TypeIdItem>();
        int typeStartIndex=0;
        while(typeStartIndex<str.length()){
            switch(str.charAt(typeStartIndex)){
                case 'Z':
                case 'B':
                case 'S':
                case 'C':
                case 'I':
                case 'J':
                case 'F':
                case 'D':
                    typeList.add(TypeIdItem.internTypeIdItem(
                                dexFile,
                                str.substring(typeStartIndex,++typeStartIndex)
                                )
                            );
                    break;
                case 'L':
                    int i=typeStartIndex;
                    while(str.charAt(++typeStartIndex) != ';');
                    typeList.add(TypeIdItem.internTypeIdItem(
                                dexFile,
                                str.substring(i,++typeStartIndex)
                                )
                            );
                    break;
                case '[':
                    int j=typeStartIndex;
                    while(str.charAt(++typeStartIndex) == '[');
                    if(str.charAt(typeStartIndex++) == 'L'){
                        while(str.charAt(typeStartIndex++) != ';');
                    }
                    typeList.add(TypeIdItem.internTypeIdItem(
                                dexFile,
                                str.substring(j,typeStartIndex)
                                )
                            );
                    break;
                default:
                    throw new RuntimeException("Invalid type "+str.substring(typeStartIndex));
            }
        }
        if(typeList.size() ==0)
            return null;

        return TypeListItem.internTypeListItem(
                dexFile,
                typeList
                );
    }


    private int[] tryItemLabels(){
        TryItem[] tries=code.tries;
        if(tries !=null&&tries.length >0){
            SparseIntArray buf=new SparseIntArray(3);
            for(TryItem tryItem :tries){
                int start=tryItem.getStartCodeAddress();
                int end=start + tryItem.getTryLength();
                //System.out.println("start: "+start);
               // System.out.println("end: "+Integer.toHexString(end));

                buf.put(start,0);
                buf.put(end,0);
                EncodedCatchHandler catchHandler=tryItem.encodedCatchHandler;
                int catchAll=catchHandler.getCatchAllHandlerAddress();
                //System.out.println("catchAll: "+catchAll);
                if(catchAll != -1){
                    buf.put(catchAll,0);
                }else{
                    EncodedTypeAddrPair[] typeAddrPairs=catchHandler.handlers;
                    for(EncodedTypeAddrPair typeAddrPair: typeAddrPairs){
                        buf.put(typeAddrPair.getHandlerAddress(),0);
                        //System.out.println("catch: "+typeAddrPair.getHandlerAddress());
                    }
                }
            }
            return buf.keys();
        }
        return new int[]{-1};
    }

    private void writeTryItems(IndentingWriter2 writer)throws IOException{
        TryItem[] tries=code.tries;
        if(tries !=null&&tries.length >0){

            writer.write('\n');
            writer.write('\n');
            writer.write("#Handler Exceptions");
            writer.write('\n');
            writer.write('\n');

            for(TryItem tryItem :tries){
                int start=tryItem.getStartCodeAddress();
                int end=start + tryItem.getTryLength();
                //System.out.println("start: "+start);
               // System.out.println("end: "+end);

                EncodedCatchHandler catchHandler=tryItem.encodedCatchHandler;
                int catchAll=catchHandler.getCatchAllHandlerAddress();
           //     System.out.println("catchAll: "+catchAll);
                if(catchAll != -1){
                    writer.write(CATCH);
                    writer.write(ALL);
                    writer.write('\n');
                    //start
                    writer.indent(4);

                    writer.write(START);
                    writer.write(LABEL);
                    writer.printIntAsDec(start);
                    writer.write('\n');
                    //end
                    writer.write(END);
                    writer.write(LABEL);
                    writer.printIntAsDec(end);
                    writer.write('\n');
                    //handler
                    writer.write(HANDLER);
                    writer.write(LABEL);
                    writer.printIntAsDec(catchAll);
                    writer.write('\n');

                    writer.deindent(4);

                    writer.write(ENDCATCH);
                    writer.write('\n');
                    writer.write('\n');
                //System.out.println("catchAll: "+catchAll);
                }else{
                    EncodedTypeAddrPair[] typeAddrPairs=catchHandler.handlers;
                    for(EncodedTypeAddrPair typeAddrPair: typeAddrPairs){
                        writer.write(CATCH);
                        writer.write(typeAddrPair.exceptionType.getTypeDescriptor());
                        writer.write('\n');

                        writer.indent(4);
                        //start
                        writer.write(START);
                        writer.write(LABEL);
                        writer.printIntAsDec(start);
                        writer.write('\n');
                        //end
                        writer.write(END);
                        writer.write(LABEL);
                        writer.printIntAsDec(end);
                        writer.write('\n');
                        //handler
                        writer.write(HANDLER);
                        writer.write(LABEL);
                        writer.printIntAsDec(typeAddrPair.getHandlerAddress());
                        writer.write('\n');

                        writer.deindent(4);

                        writer.write(ENDCATCH);
                        writer.write('\n');
                        writer.write('\n');
                                        //System.out.println("catch: "+typeAddrPair.getHandlerAddress());
                    }
                }
            }
        }
    }

    private static Pair<List<TryItem>,List<EncodedCatchHandler>> parseCatchs(DexFile dexFile,String[] strings,int index,HashMap<String,Integer> labelsMapOffsets){
        TryListBuilder tryList=new TryListBuilder();
        for(;index<strings.length;index++){
            String string=strings[index].trim();
            if(string.startsWith(".catch")){
                String[] sp=string.split(" ");
                if(sp.length<2)
                    throw new IllegalArgumentException( "no exception type : "+strings[index]);
                String exceptionType=sp[1].trim();
                //System.out.println(exceptionType);
                //start
                String tryString=strings[++index].trim();
                //System.out.println(tryString);
                String[] tryCatchs=tryString.split(":");
                int start=labelsMapOffsets.get(tryCatchs[1].trim());
                //System.out.println(""+start);
                //end
                tryString=strings[++index].trim();
               // System.out.println(tryString);
                tryCatchs=tryString.split(":");
                int end=labelsMapOffsets.get(tryCatchs[1].trim());
               // System.out.println(""+end);
                //handler
                tryString=strings[++index].trim();
                //System.out.println(tryString);
                tryCatchs=tryString.split(":");
                int handler=labelsMapOffsets.get(tryCatchs[1].trim());
               // System.out.println(""+handler);
                if(exceptionType.equals(ALL)){
                    tryList.addCatchAllHandler(start,end,handler);
                }else{
                    tryList.addHandler(TypeIdItem.internTypeIdItem(
                                dexFile,
                                exceptionType),start,end,handler);
                }
            }
        }
        return tryList.encodeTries();
    }

    public static boolean searchStringInMethod(ClassDataItem.EncodedMethod method,String src){
        if(method.codeItem!=null){
            Instruction[] instructions=method.codeItem.getInstructions();
            for(Instruction instruction:instructions){
                switch(instruction.getFormat()){
                    case Format21c:
                    case Format31c:
                        switch(instruction.opcode.referenceType){
                            case string:
                                InstructionWithReference ref=(InstructionWithReference)instruction;
                                String string=((StringIdItem)ref.getReferencedItem()).getStringValue();
                                if(string.indexOf(src)!=-1)
                                    return true;
                        }
                }
            }
        }
        return false;
    }
    public static boolean searchMethodInMethod(ClassDataItem.EncodedMethod method,String classType,String name,String descriptor,boolean ignoreNameAndDescriptor,boolean ignoreDescriptor){
        if(method.codeItem!=null){
            Instruction[] instructions=method.codeItem.getInstructions();
            for(Instruction instruction:instructions){
                switch(instruction.getFormat()){
                    case Format35c:
                    case Format35s:
                    case Format3rc:
                        switch(instruction.opcode.referenceType){
                            case method:
                                InstructionWithReference ref=(InstructionWithReference)instruction;
                                MethodIdItem item=((MethodIdItem)ref.getReferencedItem());
                                if(ignoreNameAndDescriptor&&item.getContainingClass().getTypeDescriptor().equals(classType)){
                                    return true;
                                }
                                if(ignoreDescriptor&&item.getContainingClass().getTypeDescriptor().equals(classType)
                                        &&item.getMethodName().getStringValue().equals(name)){
                                    return  true;
                                        }
                                if(item.getContainingClass().getTypeDescriptor().equals(classType)
                                        &&item.getMethodName().getStringValue().equals(name)
                                        &&item.getPrototype().getPrototypeString().equals(descriptor)){
                                    return true;
                                }
                        }
                }
            }
        }
        return false;
    }

    public static boolean searchFieldInMethod(ClassDataItem.EncodedMethod method,String classType,String name,String descriptor,boolean ignoreNameAndDescriptor,boolean ignoreDescriptor){
        if(method.codeItem!=null){
            Instruction[] instructions=method.codeItem.getInstructions();
            for(Instruction instruction:instructions){
                switch(instruction.getFormat()){
                    case Format21c:
                    case Format22c:
                        switch(instruction.opcode.referenceType){
                            case field:
                                InstructionWithReference ref=(InstructionWithReference)instruction;
                                FieldIdItem item=(FieldIdItem)ref.getReferencedItem();
                                if(ignoreNameAndDescriptor&&item.getContainingClass().getTypeDescriptor().equals(classType)){
                                    return true;
                                }
                                if(ignoreDescriptor&&item.getContainingClass().getTypeDescriptor().equals(classType)
                                        &&item.getFieldName().getStringValue().equals(name)){
                                    return true;
                                }
                                if(item.getContainingClass().getTypeDescriptor().equals(classType)
                                        &&item.getFieldName().getStringValue().equals(name)
                                        &&item.getFieldType().getTypeDescriptor().equals(descriptor)){
                                    return true;
                                }
                        }
                }
            }
        }
        return false;
    }


}
