/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package editor.cli;

import common.Util;
import editor.Main;
import static editor.Main.PREF_ROOT_KEY;
import static editor.cli.Common.loadPage;
import editor.domain.Node;
import editor.domain.ProjectData;
import editor.domain.ProjectPage;
import editor.domain.elements.GspnPage;
import editor.domain.elements.Transition;
import editor.domain.grammar.ExprLangBaseVisitor;
import editor.domain.grammar.ExprLangParser;
import editor.domain.grammar.ParserContext;
import java.io.File;
import editor.domain.io.CppFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import latex.DummyLatexProvider;

/**
 *
 * @author Irene
 * Main to call the function to write the c++ file for general transition (exp and non exp)
 */
public class CppCommand {

    public static void main(String[] args) throws Exception {

        try {
            Main.useGuiLogWindowForExceptions = false;
            Main.fixedUiSize = Main.UiSize.NORMAL;
            DummyLatexProvider.initializeProvider();
//            long totalStart = System.currentTimeMillis();
            toolMain(args);

            //System.out.println("TOTAL TIME: " + (System.currentTimeMillis() - totalStart) / 1000.0);
            //System.out.println("OK.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("EXCEPTION RAISED.");
            System.out.println("FAILED.");
            System.exit(1);
        }
    }

    public static boolean toolMain(String[] args) throws Exception {

        Util.initApplication(PREF_ROOT_KEY, "/org/unito/mainprefs");
        // Read command line arguments
        if (args.length < 2) {
            System.out.println("Not enough arguments.");
            System.exit(1);
        }

        // GREATSPN_BINDIR=~/tesiMagistrale/SOURCESC-readFromFile/JavaGUI/Editor/dist/
        // java -ea -cp ${GREATSPN_BINDIR}/Editor.jar:${GREATSPN_BINDIR}/lib/antlr-runtime-4.2.1.jar editor.cli.CppCommand EsempiExpMTDep out.cpp
        String inBaseName = args[0];
        String outBaseName = args[1];
        
        // Parse the remaining arguments
        boolean fluxBalanceFlag = false;
        for (int i=2; i<args.length; i++) {
            if (args[i].equals("-flux")) {
                fluxBalanceFlag = true;
                System.out.println("Enabling flux balance.");
            }
            else {
                System.err.println("Unknown argument: "+args[i]);
                System.exit(1);
            }
        }

        GspnPage gspn = loadPage(inBaseName);
        ArrayList<ProjectPage> pages = new ArrayList<>();
        pages.add(gspn);
        ProjectData proj = new ProjectData("project", pages);
        gspn.preparePageCheck();
        gspn.checkPage(proj, null, gspn, null);
        ParserContext context = new ParserContext(gspn);
        context.cppForFluxBalance = fluxBalanceFlag;
        gspn.compileParsedInfo(context);

        File cppFile = new File(outBaseName);

        // Enumerate the filenames mentioned in every transition delay expressions
        ExternalTermsVisitor etv = new ExternalTermsVisitor();
        for (Node n : gspn.nodes) {
            if (n instanceof Transition) {
                Transition trn = (Transition) n;
                Object obj = trn.visitDelayTree(context, etv);
            }
        }

        long saveStart = System.currentTimeMillis();
         // Convert in C++
        CppFormat.export(cppFile, gspn, fluxBalanceFlag, context, etv.filenames, etv.rnames, gspn.getPageName());

        System.out.println("SAVING TIME: " + (System.currentTimeMillis() - saveStart) / 1000.0);
        return true;

    }

    //static class to visit and collect element from formulas
    static class ExternalTermsVisitor extends ExprLangBaseVisitor<Object> {

        final Set<String> filenames = new TreeSet<>();
        final Set<String> rnames = new TreeSet<>();

        @Override
        public Object visitRealExprFromList(ExprLangParser.RealExprFromListContext ctx) {
            filenames.add(ctx.fname.getText());
            return super.visitRealExprFromList(ctx);
        }

        @Override
        public Object visitRealExprFromTable(ExprLangParser.RealExprFromTableContext ctx) {
            filenames.add(ctx.fname.getText());
            return super.visitRealExprFromTable(ctx);
        }

        /*@Override
        public Object visitRealExprFBA(ExprLangParser.RealExprFBAContext ctx) {
            filenames.add(ctx.fname.getText());
           	rnames.add(ctx.rname.getText());
           	
           	//System.out.println("Aggiungo: " + ctx.rname.getText());
           	
            return super.visitRealExprFBA(ctx);
        } */

    }

}
