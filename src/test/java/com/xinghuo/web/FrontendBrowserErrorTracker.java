package com.xinghuo.web;

import com.microsoft.playwright.Page;
import java.util.*;

/** WebKit's PageError also contains JavaScript-source network console diagnostics.
 * Use standard browser error/unhandledrejection events to retain every uncaught application
 * exception, including exceptions whose text happens to look like a cancelled N5 fetch.
 * Raw network diagnostics remain available; other PageError events remain failures too. */
final class FrontendBrowserErrorTracker {
    private static final String INSTALL="""
        (() => {
          if(window.__xhTestErrorCaptureReady)return;
          window.__xhTestErrorCaptureReady=true;
          const report=value=>Promise.resolve(window.__xhTestUncaught(String(value))).catch(()=>{});
          window.addEventListener('error',event=>{
            if(event instanceof ErrorEvent)report(event.error?.stack||event.message);
          });
          window.addEventListener('unhandledrejection',event=>report(event.reason?.stack||event.reason));
        })();
        """;
    private final List<String> unexpected;
    private final List<String> raw=new ArrayList<>();
    FrontendBrowserErrorTracker(List<String> unexpected){this.unexpected=unexpected;}
    void attach(Page page){
        page.exposeFunction("__xhTestUncaught",args->{unexpected.add(String.valueOf(args[0]));return null;});
        page.addInitScript(INSTALL);
        page.evaluate(INSTALL);
        page.onPageError(error->{
            if(raw.size()==40)raw.removeFirst();raw.add(error);
            if(!isN5NetworkDiagnostic(error))unexpected.add(error);
        });
    }
    List<String> events(){return List.copyOf(raw);}
    private static boolean isN5NetworkDiagnostic(String error){
        return error.contains("/next/n5.js")&&error.contains("Fetch API cannot load ")&&error.contains("due to access control checks.");
    }
}
