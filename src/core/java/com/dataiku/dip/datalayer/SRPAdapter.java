package com.dataiku.dip.datalayer;


/** 
 * Small helper to fit a SingleRowProcessor in a SingleInputSingleOutput processor.
 * It simply processes the row and emit it in its output if it's not deleted.
 */
public class SRPAdapter extends SingleInputSingleOutputRowProcessor{
    SingleRowProcessor p;

    public SRPAdapter(SingleRowProcessor p) {
        this.p = p;
    }

    public String toString() {
        return "SRPAdapter[" + p.toString() + "]";
    }

    @Override
    public void processRow(Row row) throws Exception {
//        if (p.getClass().getName().contains("RemoveRows")) System.out.println("Processor " + p + " processes " + row);
        p.processRow(row);
        if (out == null) {
            throw new Error("No output for " + p);
        }
//        if (row.isDeleted()) {
//            throw new Error("Deleted by " + p);
//        }
        if (out != null && !row.isDeleted()) {
//            if (p.getClass().getName().contains("RemoveRows"))System.out.println("  and emits to "+ out);
            out.emitRow(row);
        } else  {
//            if (p.getClass().getName().contains("RemoveRows"))System.out.println("   and dropped");
        }
    }

    @Override
    public void init() throws Exception {
        p.setColumnFactory(cf);
        p.init();
    }

    @Override
    public void postProcess() throws Exception {
        p.postProcess();
        if (out != null) {
            out.lastRowEmitted();
        }
    }
}