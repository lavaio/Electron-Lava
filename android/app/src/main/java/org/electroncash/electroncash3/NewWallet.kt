package org.electroncash.electroncash3

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.text.Selection
import android.view.View
import android.widget.Toast
import com.chaquo.python.Kwarg
import com.chaquo.python.PyException
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.new_wallet.*
import kotlinx.android.synthetic.main.new_wallet_2.*
import kotlin.properties.Delegates.notNull


val libKeystore by lazy { libMod("keystore") }
val libWallet by lazy { libMod("wallet") }


class NewWalletDialog1 : AlertDialogFragment() {
    override fun onBuildDialog(builder: AlertDialog.Builder) {
        builder.setTitle(R.string.New_wallet)
            .setView(R.layout.new_wallet)
            .setPositiveButton(R.string.next, null)
            .setNegativeButton(R.string.cancel, null)
    }

    override fun onShowDialog() {
        spnType.adapter = MenuAdapter(context!!, R.menu.wallet_type)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            try {
                val name = etName.text.toString()
                if (name.isEmpty()) throw ToastException(R.string.name_is, Toast.LENGTH_SHORT)
                if (name.contains("/")) throw ToastException(R.string.invalid_name)
                if (daemonModel.listWallets().contains(name)) {
                    throw ToastException(R.string.a_wallet_with_that_name_already_exists_please)
                }
                val password = confirmPassword(dialog)

                val nextDialog: DialogFragment
                val arguments = Bundle().apply {
                    putString("name", name)
                    putString("password", password)
                }

                val walletType = spnType.selectedItemId.toInt()
                if (walletType in listOf(R.id.menuCreateSeed, R.id.menuRestoreSeed)) {
                    nextDialog = NewWalletSeedDialog()
                    val seed = if (walletType == R.id.menuCreateSeed)
                                   daemonModel.commands.callAttr("make_seed").toString()
                               else null
                    arguments.putString("seed", seed)
                } else if (walletType == R.id.menuImport) {
                    nextDialog = NewWalletImportDialog()
                } else if (walletType == R.id.menuImportMaster) {
                    nextDialog = NewWalletImportMasterDialog()
                } else {
                    throw Exception("Unknown item: ${spnType.selectedItem}")
                }
                showDialog(activity!!, nextDialog.apply { setArguments(arguments) })
            } catch (e: ToastException) { e.show() }
        }
    }
}


fun confirmPassword(dialog: Dialog): String {
    val password = dialog.etPassword.text.toString()
    if (password.isEmpty()) throw ToastException(R.string.Enter_password, Toast.LENGTH_SHORT)
    if (password != dialog.etConfirmPassword.text.toString()) {
        throw ToastException(R.string.wallet_passwords)
    }
    return password
}


abstract class NewWalletDialog2 : TaskLauncherDialog<Unit>() {
    var input: String by notNull()

    override fun onBuildDialog(builder: AlertDialog.Builder) {
        builder.setTitle(R.string.New_wallet)
            .setView(R.layout.new_wallet_2)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(R.string.back, null)
    }

    override fun onPreExecute() {
        input = etInput.text.toString()
    }

    override fun doInBackground() {
        val name = arguments!!.getString("name")!!
        val password = arguments!!.getString("password")!!
        onCreateWallet(name, password)
        daemonModel.loadWallet(name, password)
    }

    abstract fun onCreateWallet(name: String, password: String)

    override fun onPostExecute(result: Unit) {
        dismissDialog(activity!!, NewWalletDialog1::class)
    }
}


class NewWalletSeedDialog : NewWalletDialog2() {
    var passphrase: String by notNull()
    var bip39: Boolean by notNull()
    var derivation: String? = null

    override fun onShowDialog() {
        super.onShowDialog()
        setupSeedDialog(this)
        if (arguments!!.getString("seed") == null) {  // Restore from seed
            bip39Panel.visibility = View.VISIBLE
            swBip39.setOnCheckedChangeListener { _, isChecked ->
                etDerivation.isEnabled = isChecked
            }
        }
    }

    override fun onPreExecute() {
        super.onPreExecute()
        passphrase = etPassphrase.text.toString()
        bip39 = swBip39.isChecked
        if (bip39) {
            derivation = etDerivation.text.toString()
        }
    }

    override fun onCreateWallet(name: String, password: String) {
        try {
            if (derivation != null &&
                !libBitcoin.callAttr("is_bip32_derivation", derivation).toBoolean()) {
                throw ToastException(R.string.Derivation_invalid)
            }
            daemonModel.commands.callAttr(
                "create", name, password,
                Kwarg("seed", input),
                Kwarg("passphrase", passphrase),
                Kwarg("bip39_derivation", derivation))
        } catch (e: PyException) {
            if (e.message!!.startsWith("InvalidSeed")) {
                throw ToastException(R.string.the_seed_you_entered_does_not_appear)
            }
            throw e
        }
    }
}


class NewWalletImportDialog : NewWalletDialog2() {
    override fun onBuildDialog(builder: AlertDialog.Builder) {
        super.onBuildDialog(builder)
        builder.setNeutralButton(R.string.qr_code, null)
    }

    override fun onShowDialog() {
        super.onShowDialog()
        tvPrompt.setText(R.string.enter_a_list_of_lava)
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { scanQR(this) }
    }

    override fun onCreateWallet(name: String, password: String) {
        var foundAddress = false
        var foundPrivkey = false
        for (word in input.split(Regex("\\s+"))) {
            if (word.isEmpty()) {
                // Can happen at start or end of list.
            } else if (clsAddress.callAttr("is_valid", word).toBoolean()) {
                foundAddress = true
            } else if (libBitcoin.callAttr("is_private_key", word).toBoolean()) {
                foundPrivkey = true
            } else {
                throw ToastException(getString(R.string.not_a_valid, word))
            }
        }

        if (foundAddress) {
            if (foundPrivkey) {
                throw ToastException(
                    R.string.cannot_specify_private_keys_and_addresses_in_the_same_wallet)
            }
            daemonModel.commands.callAttr("create", name, password, Kwarg("addresses", input))
        } else if (foundPrivkey) {
            daemonModel.commands.callAttr("create", name, password, Kwarg("privkeys", input))
        } else {
            throw ToastException(R.string.you_appear_to_have_entered_no)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val text = etInput.text
            if (!text.isEmpty() && !text.endsWith("\n")) {
                text.append("\n")
            }
            text.append(result.contents)
            Selection.setSelection(text, text.length)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}


class NewWalletImportMasterDialog : NewWalletDialog2() {
    override fun onBuildDialog(builder: AlertDialog.Builder) {
        super.onBuildDialog(builder)
        builder.setNeutralButton(R.string.qr_code, null)
    }

    override fun onShowDialog() {
        super.onShowDialog()
        tvPrompt.setText(getString(R.string.to_create_a_watching) + " " +
                                getString(R.string.to_create_a_spending))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { scanQR(this) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            etInput.setText(result.contents)
            etInput.setSelection(result.contents.length)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateWallet(name: String, password: String) {
        val key = input.trim()
        if (libKeystore.callAttr("is_bip32_key", key).toBoolean()) {
            daemonModel.commands.callAttr("create", name, password, Kwarg("master", key))
        } else {
            throw ToastException(R.string.please_specify)
        }
    }
}


fun setupSeedDialog(fragment: AlertDialogFragment) {
    with (fragment) {
        val seed = fragment.arguments!!.getString("seed")
        if (seed == null) {
            // Import
            tvPrompt.setText(R.string.please_enter_your_seed_phrase)
        } else {
            // Generate or display
            tvPrompt.setText(seedAdvice(seed))
            etInput.setText(seed)
            etInput.setFocusable(false)
        }

        val passphrase = fragment.arguments!!.getString("passphrase")
        if (passphrase == null) {
            // Import or generate
            passphrasePanel.visibility = View.VISIBLE
            tvPassphrasePrompt.setText(R.string.please_enter_your_seed_derivation)
        } else {
            // Display
            if (passphrase.isNotEmpty()) {
                passphrasePanel.visibility = View.VISIBLE
                tvPassphrasePrompt.setText(R.string.passphrase)
                etPassphrase.setText(passphrase)
                etPassphrase.setFocusable(false)
            }
        }
    }
}


fun seedAdvice(seed: String): String {
    return app.getString(R.string.please_save, seed.split(" ").size) + " " +
           app.getString(R.string.this_seed) + " " +
           app.getString(R.string.never_disclose)
}
