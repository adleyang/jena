/*
 * (c) Copyright 2011 Epimorphics Ltd.
 * All rights reserved.
 * [See end of file]
 */

package tx;

import static com.hp.hpl.jena.tdb.index.IndexTestLib.testInsert ;
import static org.openjena.atlas.test.Gen.strings ;

import java.util.Iterator ;

import org.junit.Test ;
import org.openjena.atlas.lib.Bytes ;
import org.openjena.atlas.lib.FileOps ;
import org.openjena.atlas.logging.Log ;
import org.openjena.atlas.test.Gen ;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;
import tx.transaction.TransactionManager ;

import com.hp.hpl.jena.query.DatasetFactory ;
import com.hp.hpl.jena.query.Query ;
import com.hp.hpl.jena.query.QueryExecution ;
import com.hp.hpl.jena.query.QueryExecutionFactory ;
import com.hp.hpl.jena.query.QueryFactory ;
import com.hp.hpl.jena.query.Syntax ;
import com.hp.hpl.jena.sparql.core.DatasetGraph ;
import com.hp.hpl.jena.sparql.sse.SSE ;
import com.hp.hpl.jena.sparql.util.QueryExecUtils ;
import com.hp.hpl.jena.tdb.base.block.Block ;
import com.hp.hpl.jena.tdb.base.block.BlockMgr ;
import com.hp.hpl.jena.tdb.base.block.BlockMgrFactory ;
import com.hp.hpl.jena.tdb.base.block.BlockMgrLogger ;
import com.hp.hpl.jena.tdb.base.block.BlockMgrTracker ;
import com.hp.hpl.jena.tdb.base.file.Location ;
import com.hp.hpl.jena.tdb.base.record.Record ;
import com.hp.hpl.jena.tdb.base.record.RecordFactory ;
import com.hp.hpl.jena.tdb.base.record.RecordLib ;
import com.hp.hpl.jena.tdb.base.recordbuffer.RecordBufferPage ;
import com.hp.hpl.jena.tdb.index.IndexTestLib ;
import com.hp.hpl.jena.tdb.index.RangeIndex ;
import com.hp.hpl.jena.tdb.index.bplustree.BPTreeNode ;
import com.hp.hpl.jena.tdb.index.bplustree.BPlusTree ;
import com.hp.hpl.jena.tdb.index.bplustree.BPlusTreeParams ;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB ;
import com.hp.hpl.jena.update.UpdateAction ;
import com.hp.hpl.jena.update.UpdateFactory ;
import com.hp.hpl.jena.update.UpdateRequest ;

public class TxMain
{
    static { Log.setLog4j() ; }
    static String divider = "----------" ;
    static String nextDivider = null ;
    static void divider()
    {
        if ( nextDivider != null )
            System.out.println(nextDivider) ;
        nextDivider = divider ;
    }
    
    static void exit(int rc)
    {
        System.out.println("EXIT") ;
        System.exit(rc) ;
    }
    
    public static void main(String... args)
    {
        //tree_ins_2_01() ; exit(0) ;
        
        //test.BPlusTreeRun.main("test", "--bptree:track", "3", "125", "100000") ; exit(0) ;
        //test.BPlusTreeRun.main("perf", "3", "125", "100") ; exit(0) ;
        //test.BPlusTreeRun.main("test", "--bptree:check", "3", "125", "100000") ; exit(0) ;
        
        
        BlockMgr blkMgr = BlockMgrFactory.createMem("test", 100) ;
        blkMgr = BlockMgrTracker.track("foo", blkMgr) ;
        blkMgr.beginUpdate() ;
        Block block = blkMgr.allocate(-1) ;
        blkMgr.release(block) ;
        blkMgr.release(block) ; // Need to check where an active bloock is - promote is one read -> write? 
        Block block2 = blkMgr.allocate(-1) ;
        blkMgr.endUpdate() ;
      
        exit(0) ;
        
        
        
        bpTreeTracking() ; exit(0) ;
        
        transactional() ; exit(0) ;
    }
    
    @Test public static void tree_ins_2_01() 
    {
        int[] keys = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        RangeIndex rIndex = makeRangeIndex(2) ;
        testInsert(rIndex, keys) ;
//        assertEquals(0, r(rIndex.minKey())) ;
//        assertEquals(9, r(rIndex.maxKey())) ;
    }
    
    
    //@Override
    protected static RangeIndex makeRangeIndex(int order)
    {
        return makeRangeIndex(order, order) ;
    }
    
    protected static RangeIndex makeRangeIndex(int order, int minRecords)
    {
        BPlusTree bpt = BPlusTree.makeMem(order, minRecords, RecordLib.TestRecordLength, 0) ;
        bpt = BPlusTree.addTracking(bpt) ;
        return bpt ; 
    }
    
    public static RangeIndex make(int order, int[] vals)
    {
        RangeIndex rIndex = makeRangeIndex(2) ;
        IndexTestLib.add(rIndex, vals) ;
        return rIndex ;
    }
    
    public static void test()
    {
        int[] keys = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        RangeIndex rIndex = make(2, keys) ;
        Iterator<Record> iter1 = rIndex.iterator() ;
        Iterator<Record> iter2 = rIndex.iterator() ;
        iter1.next() ;
        iter2.next() ;
    }
    
    public static void bpTreeTracking(String... args)
    {
        final Logger log = LoggerFactory.getLogger("BPlusTree") ;

        int order = 3 ;
        int keySize = RecordLib.TestRecordLength ;
        int valueSize = 0 ;
        boolean blkTracking = true ;
        boolean blkLogging = false ;
        int numKeys = 10 ;
        int maxValue = 100 ;

        int[] keys1 = Gen.rand(numKeys, 0, maxValue) ;
        int[] keys2 = Gen.permute(keys1, numKeys) ;
        
        System.out.printf("int[] keys1 = {%s} ;\n", strings(keys1)) ;
        System.out.printf("int[] keys2 = {%s} ; \n", strings(keys2)) ;
        
        // Debug options.
        if ( false )
        {
            // These alter the block behaviour.
//            SystemTDB.Checking = true ;
//            BPlusTreeParams.CheckingNode = true ;
//            BPlusTreeParams.CheckingTree = true ;
            BPlusTreeParams.Logging = true ;
            Log.enable(BPTreeNode.class.getName(), "ALL") ;
        }
        RecordFactory rf = new RecordFactory(keySize,valueSize) ;
        BPlusTree bpTree = createBPT(order, rf, blkTracking, blkLogging) ;
        System.out.println("CheckingNode: "+BPlusTreeParams.CheckingNode) ;
        System.out.println("CheckingTree: "+BPlusTreeParams.CheckingTree) ;
        
        String label = "B+Tree" ;
        BPlusTreeParams params = bpTree.getParams() ;
        System.out.println(label+": "+params) ;
        int blockSize  = BPlusTreeParams.calcBlockSize(order, rf) ;
        System.out.println("Block size = "+params.getCalcBlockSize()) ;
        System.out.println() ;

        log.info("ADD") ;
        for ( int i : keys1 ) 
        {
//            log.info(String.format("i = 0x%04X", i)) ;
            Record r = record(rf, i, 0) ;
            bpTree.add(r) ;
        }
        
        //exit(0) ;
        
        if ( false )
        {
            System.out.println() ;
            log.info("DELETE") ;
            for ( int i : keys2 ) 
            {
    //            System.out.println() ;
    //            log.info(String.format("i = 0x%04X", i)) ;
    //            bpTree.dump() ;
                Record r = record(rf, i, 0) ;
                bpTree.delete(r) ;
            }
        }

        //exit(0) ;

        System.out.println() ;
        log.info("ITERATOR") ;
        Iterator<Record> iter = bpTree.iterator() ;
        for ( ; iter.hasNext() ; )
            System.out.println(iter.next()) ;
        System.out.println() ;

        //        bpt.dump() ;
    }

    private static BPlusTree createBPT(int order, RecordFactory rf, boolean tracking, boolean logging)
    {
        String label = "B+Tree" ;
        BPlusTreeParams params = new BPlusTreeParams(order, rf) ;
        int blockSize  = BPlusTreeParams.calcBlockSize(order, rf) ;
        
        if ( false )
        {
            BPlusTree rIndex = BPlusTree.makeMem(order, order, RecordLib.TestRecordLength, 0) ;
            return BPlusTree.addTracking(rIndex) ;
        }
            
        int nodeBlkSize = blockSize ;
        int maxRecords = 2*order ;
        // Or blockSize
        int recBlkSize = RecordBufferPage.calcBlockSize(params.getRecordFactory(), maxRecords) ;
        
        BlockMgr mgr1 = BlockMgrFactory.createMem("B1", nodeBlkSize) ;
        
        if ( tracking )
            mgr1 = new BlockMgrTracker("BlkMgr/Nodes", mgr1) ;
        if ( logging )
            mgr1 = new BlockMgrLogger("BlkMgr/Nodes", mgr1, true) ;
        
        BlockMgr mgr2 = BlockMgrFactory.createMem("B2", recBlkSize) ;
    
        if ( tracking )
            mgr2 = new BlockMgrTracker("BlkMgr/Records", mgr2) ;
        if ( logging )
            mgr2 = new BlockMgrLogger("BlkMgr/Records", mgr2, true) ;
        
        BPlusTree bpt = BPlusTree.create(params, mgr1, mgr2) ;
        return bpt ;
    }

    public static void transactional(String... args)
    {
        Location location ;
        if ( false )
        {
            String dirname = "DBX" ;
            if ( false && FileOps.exists(dirname) )
                FileOps.clearDirectory(dirname) ;
            location = new Location(dirname) ;
        } else
            location = Location.mem() ;

        TransactionManager txnMgr = new TransactionManager() ;
        DatasetGraphTDB dsg = txnMgr.build(location) ;
        //dsg.add(SSE.parseQuad("(_ <s> <p> 'o')")) ;
        
        DatasetGraphTxView dsgX1 = txnMgr.begin(dsg) ;
        dsgX1.add(SSE.parseQuad("(_ <sx> <px> 'ox1')")) ;
        
//        System.out.println("Base:") ;
//        //System.out.println(dsg) ;
//        query("SELECT count(*) { ?s ?p ?o }", dsg) ;
        
        System.out.println("Transaction:") ;
        //System.out.println(dsgX) ;
        query("SELECT count(*) { ?s ?p ?o }", dsgX1) ;
        update("CLEAR DEFAULT", dsgX1) ;
        query("SELECT count(*) { ?s ?p ?o }", dsgX1) ;
        
        System.out.println("Base:") ;
        //System.out.println(dsg) ;
        query("SELECT count(*) { ?s ?p ?o }", dsg) ;
        
        DatasetGraphTxView dsgX2 = txnMgr.begin(dsg) ;
        dsgX2.add(SSE.parseQuad("(_ <sx> <px> 'ox2')")) ;

        
        System.out.println("Transaction:") ;
        //System.out.println(dsgX) ;
        query("SELECT count(*) { ?s ?p ?o }", dsgX1) ;
        dsgX1.abort() ;
        
        System.out.println("Done") ;
        System.exit(0) ;
        
    }
    
    public static void query(String queryStr, DatasetGraph dsg)
    {
        Query query = QueryFactory.create(queryStr, Syntax.syntaxARQ) ;
        QueryExecution qExec = QueryExecutionFactory.create(query, DatasetFactory.create(dsg)) ;
        QueryExecUtils.executeQuery(query, qExec) ;
    }
    
    public static void update(String updateStr, DatasetGraph dsg)
    {
        UpdateRequest req = UpdateFactory.create(updateStr) ;
        UpdateAction.execute(req, dsg) ;
    }
    
    static Record record(RecordFactory rf, int key, int val)
    {
        Record r = rf.create() ;
        Bytes.setInt(key, r.getKey()) ;
        if ( rf.hasValue() )
            Bytes.setInt(val, r.getValue()) ;
        return r ;
    }
}

/*
 * (c) Copyright 2011 Epimorphics Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */