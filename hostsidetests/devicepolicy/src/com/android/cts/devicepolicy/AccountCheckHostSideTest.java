/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.cts.devicepolicy;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.log.LogUtil.CLog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccountCheckHostSideTest extends BaseDevicePolicyTest {
    private static final String APK_NON_TEST_ONLY = "CtsAccountCheckNonTestOnlyOwnerApp.apk";
    private static final String APK_TEST_ONLY = "CtsAccountCheckTestOnlyOwnerApp.apk";
    private static final String APK_AUTH = "CtsAccountCheckAuthApp.apk";

    private static final String PACKAGE_NON_TEST_ONLY =
            "com.android.cts.devicepolicy.accountcheck.nontestonly";
    private static final String PACKAGE_TEST_ONLY =
            "com.android.cts.devicepolicy.accountcheck.testonly";
    private static final String PACKAGE_AUTH = "com.android.cts.devicepolicy.accountcheck.auth";

    private static final String OWNER_TEST_ONLY = PACKAGE_TEST_ONLY
            + "/com.android.cts.devicepolicy.accountcheck.owner.AdminReceiver";
    private static final String OWNER_NON_TEST_ONLY = PACKAGE_NON_TEST_ONLY
            + "/com.android.cts.devicepolicy.accountcheck.owner.AdminReceiver";

    private static final String TEST_CLASS =
            "com.android.cts.devicepolicy.accountcheck.AccountCheckTest";

    @Override
    protected void tearDown() throws Exception {
        if (mHasFeature) {
            runCleanupTestOnlyOwner();
            runCleanupNonTestOnlyOwner();

            // This shouldn't be needed since we're uninstalling the authenticator, but sometimes
            // the account manager fails to clean up?
            removeAllAccounts();

            getDevice().uninstallPackage(PACKAGE_AUTH);
            getDevice().uninstallPackage(PACKAGE_TEST_ONLY);
            getDevice().uninstallPackage(PACKAGE_NON_TEST_ONLY);
        }
        super.tearDown();
    }

    private void runTest(String method) throws Exception {
        assertTrue(runDeviceTests(PACKAGE_AUTH, TEST_CLASS, method));
    }

    private void runCleanupTestOnlyOwner() throws Exception {
        removeAdmin(OWNER_TEST_ONLY, mPrimaryUserId);
    }

    private void runCleanupNonTestOnlyOwner() throws Exception {
        runTest("testCleanUpNonTestOwner");
    }

    private void removeAllAccounts() throws Exception {
        runTest("testRemoveAllAccounts");
    }

    private void assertTestOnlyInstallable() throws Exception {
        setDeviceOwnerOrFail(OWNER_TEST_ONLY, mPrimaryUserId);
        runCleanupTestOnlyOwner();

        setProfileOwnerOrFail(OWNER_TEST_ONLY, mPrimaryUserId);
        runCleanupTestOnlyOwner();
    }

    private void assertNonTestOnlyInstallable() throws Exception {
        setDeviceOwnerOrFail(OWNER_NON_TEST_ONLY, mPrimaryUserId);
        runCleanupNonTestOnlyOwner();

        setProfileOwnerOrFail(OWNER_NON_TEST_ONLY, mPrimaryUserId);
        runCleanupNonTestOnlyOwner();
    }

    private void assertTestOnlyNotInstallable() throws Exception {
        setDeviceOwnerExpectingFailure(OWNER_TEST_ONLY, mPrimaryUserId);
        runCleanupTestOnlyOwner();

        setProfileOwnerExpectingFailure(OWNER_TEST_ONLY, mPrimaryUserId);
        runCleanupTestOnlyOwner();
    }

    private void assertNonTestOnlyNotInstallable() throws Exception {
        setDeviceOwnerExpectingFailure(OWNER_NON_TEST_ONLY, mPrimaryUserId);
        runCleanupNonTestOnlyOwner();

        setProfileOwnerExpectingFailure(OWNER_NON_TEST_ONLY, mPrimaryUserId);
        runCleanupNonTestOnlyOwner();
    }

    private boolean hasAccounts() throws Exception {
        final String accountDump = getDevice().executeShellCommand("dumpsys account");

        final Pattern p = Pattern.compile("^\\s*Accounts\\:\\s*(\\d+)", Pattern.MULTILINE);
        final Matcher m = p.matcher(accountDump);
        if (!m.find()) {
            fail("Unable to obtain # of accounts");
            return true;
        }
        final String count = m.group(1);

        CLog.i("# of preconfigured accounts=" + count);

        return Integer.parseInt(count) > 0;
    }

    public void testAccountCheck() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(APK_AUTH, mPrimaryUserId);
        installAppAsUser(APK_NON_TEST_ONLY, mPrimaryUserId);
        installAppAsUser(APK_TEST_ONLY, mPrimaryUserId);

        runCleanupTestOnlyOwner();
        runCleanupNonTestOnlyOwner();
        removeAllAccounts();
        try {
            runTest("testCheckPreconfiguredAccountFeatures");

            final boolean hasPreconfiguredAccounts = hasAccounts();

            // All pre-configured accounts must be "compatible", so the test-only owner can be
            // installed.
            assertTestOnlyInstallable();

            if (hasPreconfiguredAccounts) {
                assertNonTestOnlyNotInstallable();
            } else {
                assertNonTestOnlyInstallable();
            }

            // Incompatible, type A.
            runTest("testAddIncompatibleA");

            assertTestOnlyNotInstallable();
            assertNonTestOnlyNotInstallable();

            // Incompatible, type B.
            removeAllAccounts();
            runTest("testAddIncompatibleB");

            assertTestOnlyNotInstallable();
            assertNonTestOnlyNotInstallable();

            // Incompatible, type C.
            removeAllAccounts();
            runTest("testAddIncompatibleC");

            assertTestOnlyNotInstallable();
            assertNonTestOnlyNotInstallable();

            // Compatible.
            removeAllAccounts();
            runTest("testAddCompatible");

            assertTestOnlyInstallable(); // Now test-only owner can be accepted.
            assertNonTestOnlyNotInstallable();

            // 2 compatible accounts.
            removeAllAccounts();
            runTest("testAddCompatible");
            runTest("testAddCompatible");

            assertTestOnlyInstallable(); // Now test-only owner can be accepted.

            assertNonTestOnlyNotInstallable();

            // 2 compatible accounts + 1 incompatible.
            removeAllAccounts();
            runTest("testAddIncompatibleA");
            runTest("testAddCompatible");
            runTest("testAddCompatible");

            assertTestOnlyNotInstallable();
            assertNonTestOnlyNotInstallable();

            // 2 compatible accounts + 1 incompatible, different order.
            removeAllAccounts();
            runTest("testAddCompatible");
            runTest("testAddCompatible");
            runTest("testAddIncompatibleB");

            assertTestOnlyNotInstallable();
            assertNonTestOnlyNotInstallable();
        } catch (Throwable th) {
            CLog.w("Tests failed; current accounts are:");
            CLog.w(getDevice().executeShellCommand("dumpsys account"));

            // Dump accounts
            throw th;
        }
    }
}
