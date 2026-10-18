import importlib.util
from pathlib import Path
import unittest
import json
import tempfile
from unittest.mock import patch
import subprocess
spec = importlib.util.spec_from_file_location('usage', Path(__file__).parents[1] / 'scripts/coding-usage.py')
usage = importlib.util.module_from_spec(spec);spec.loader.exec_module(usage)
class UsageTests(unittest.TestCase):
    def setUp(self): self.now = 1792300000000
    def test_codex_duration_and_no_account_data(self):
        result = usage.codex_windows({'accountId':'never-forward','rateLimits':{'primary':{'usedPercent':8,'windowDurationMins':10080,'resetsAt':self.now//1000+3600},'secondary':None,'credits':{'secret':'no'}}},self.now)
        self.assertEqual(result['windows'][0]['durationMins'],10080)
        self.assertEqual(set(result),{'at','status','windows'})
        self.assertNotIn('never-forward',str(result))
        self.assertEqual(len(result['windows']),1)
    def test_unknown_and_reported_zero(self):
        self.assertIsNone(usage.window(None,300,self.now//1000+5,self.now))
        self.assertEqual(usage.window(0,300,self.now//1000+5,self.now)['usedPercent'],0)
        for percent in [True,float('nan'),float('inf'),-1,101]: self.assertIsNone(usage.window(percent,300,self.now//1000+5,self.now))
    def test_claude_allowlist(self):
        result=usage.claude_windows({'transcript_path':'never-forward','session_id':'never-forward','rate_limits':{'five_hour':{'used_percentage':12.5,'resets_at':self.now//1000+3600},'seven_day':{'used_percentage':None,'resets_at':None}}},self.now)
        self.assertEqual(len(result['windows']),1);self.assertNotIn('never-forward',str(result));self.assertEqual(result['windows'][0]['durationMins'],300)
        self.assertIsNone(usage.claude_windows({},self.now));self.assertIsNone(usage.claude_windows([],self.now))
    def test_reset_and_failed_refresh(self):
        self.assertIsNone(usage.window(40,300,self.now//1000-1,self.now))
        raw={'at':self.now-300000,'windows':[{'usedPercent':8,'durationMins':10080,'resetAt':self.now+3600000}]}
        saved=usage.clean_saved(raw,self.now,True)
        self.assertEqual(saved['at'],raw['at']);self.assertEqual(saved['status'],'saved')
        self.assertEqual(usage.clean_saved({'at':self.now+600000},self.now)['at'],0)
    def test_failed_refresh_keeps_age_and_only_normalized_fields(self):
        with tempfile.TemporaryDirectory() as directory:
            state=Path(directory)
            previous={'at':self.now-300000,'windows':[{'usedPercent':8,'durationMins':10080,'resetAt':self.now+3600000}],'secret':'never-forward'}
            usage.save(state/'snapshot.json',{'providers':{'codex':previous}})
            with patch.object(usage,'read_codex',side_effect=RuntimeError()), patch.object(usage.time,'time',return_value=self.now/1000):
                snapshot,published=usage.collect({'codex':'unused'},state)
            self.assertFalse(published)
            self.assertEqual(snapshot['providers']['codex']['at'],previous['at'])
            self.assertEqual(snapshot['providers']['codex']['status'],'saved')
            self.assertNotIn('never-forward',json.dumps(snapshot))
            self.assertEqual((state/'snapshot.json').stat().st_mode & 0o777,0o600)
    def test_publish_requires_positive_tv_acknowledgment(self):
        with tempfile.TemporaryDirectory() as directory:
            state=Path(directory)
            source={'rateLimits':{'primary':{'usedPercent':8,'windowDurationMins':10080,'resetsAt':self.now//1000+3600}}}
            config={'codex':'codex','adb':'adb','serial':'test-target'}
            for response,expected in [(b'Error while accessing provider',False),(b'Bundle[{accepted=true}]',True)]:
                def run(args,**kwargs):
                    return subprocess.CompletedProcess(args,0,response if 'call' in args else b'device',b'')
                with patch.object(usage,'read_codex',return_value=source), patch.object(usage.time,'time',return_value=self.now/1000), patch.object(usage.subprocess,'run',side_effect=run):
                    self.assertEqual(usage.collect(config,state)[1],expected)
    def test_malformed_sources(self):
        for value in [None, [], 'invalid']:
            with self.assertRaises(ValueError): usage.codex_windows(value,self.now)
            self.assertEqual(usage.clean_saved(value,self.now)['at'],0)
        for value in [12, {}, 'invalid']:
            self.assertEqual(usage.clean_saved({'at':self.now,'windows':value},self.now)['windows'],[])
        self.assertEqual(usage.clean_saved({'at':self.now,'windows':[{'resetAt':'invalid'}]},self.now)['windows'],[])
if __name__=='__main__': unittest.main()
