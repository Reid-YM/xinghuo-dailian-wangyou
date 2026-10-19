package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class FrontendBrowserErrorTrackerPlaywrightTest {
    private static final String NETWORK="Fetch API cannot load http://127.0.0.1:1234/api/console/summary due to access control checks.";
    @Test void actualUncaughtRuntimeErrorsRemainFailures()throws Exception{
        both((page,errors)->{page.evaluate("() => {setTimeout(()=>{throw new TypeError('actual-runtime-error')},0);}");page.waitForCondition(()->!errors.isEmpty());assertTrue(errors.stream().anyMatch(error->error.contains("actual-runtime-error")));});
    }
    @Test void thrownNetworkStyleErrorCannotBeClassifiedAway()throws Exception{
        both((page,errors)->{script(page,"setTimeout(()=>{throw new Error('"+NETWORK+"')},0);");page.waitForCondition(()->!errors.isEmpty());assertTrue(errors.stream().anyMatch(error->error.contains(NETWORK)));});
    }
    @Test void unhandledNetworkStyleRejectionsRemainFailures()throws Exception{
        both((page,errors)->{script(page,"Promise.reject(new Error('"+NETWORK+"'));");page.waitForCondition(()->!errors.isEmpty());assertTrue(errors.stream().anyMatch(error->error.contains(NETWORK)));});
    }
    @Test void handledAbortAndHandledReadRejectionAreNotUncaughtErrors()throws Exception{
        both((page,errors)->{script(page,"const stop=new AbortController();stop.abort();fetch('https://example.invalid/api/test',{signal:stop.signal}).catch(()=>{});Promise.reject(new Error('"+NETWORK+"')).catch(()=>{});");page.waitForTimeout(200);assertEquals(List.of(),errors);});
    }
    static void script(Page page,String content){page.evaluate("content=>{const script=document.createElement('script');script.textContent=content+'\\n//# sourceURL=http://127.0.0.1:1234/next/n5.js';document.head.append(script);}",content);}
    interface Check{void run(Page page,List<String> errors)throws Exception;}
    static void both(Check check)throws Exception{
        try(Playwright p=Playwright.create();Browser chromium=p.chromium().launch();Browser webkit=p.webkit().launch()){
            for(Browser browser:List.of(chromium,webkit))try(Page page=browser.newPage()){
                List<String> errors=new ArrayList<>();new FrontendBrowserErrorTracker(errors).attach(page);
                page.navigate("data:text/html,<h1>Error tracking test</h1>");check.run(page,errors);
            }
        }
    }
}
