package com.zbrowser.app.ui.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.zbrowser.app.MainActivity
import com.zbrowser.app.R
import com.zbrowser.app.ZBrowserApp
import com.zbrowser.app.data.Bookmark
import com.zbrowser.app.databinding.FragmentBookmarksBinding
import kotlinx.coroutines.launch

class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: BookmarksAdapter
    private val bookmarkDao by lazy { (requireActivity().application as ZBrowserApp).database.bookmarkDao() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeBookmarks()
    }

    private fun setupRecyclerView() {
        adapter = BookmarksAdapter(
            onBookmarkClick = { bookmark ->
                (activity as? MainActivity)?.webViewManager?.createTab(bookmark.url)
                findNavController().navigate(R.id.browserFragment)
            },
            onBookmarkLongClick = { bookmark ->
                showBookmarkOptionsDialog(bookmark)
            }
        )
        binding.recyclerBookmarks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerBookmarks.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.fabAddBookmark.setOnClickListener {
            showAddBookmarkDialog()
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeBookmarks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bookmarkDao.getAllBookmarks().collect { bookmarks ->
                    adapter.submitList(bookmarks)
                    binding.emptyState.visibility = if (bookmarks.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerBookmarks.visibility = if (bookmarks.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun showAddBookmarkDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_bookmark, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.input_title)
        val urlInput = dialogView.findViewById<TextInputEditText>(R.id.input_url)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_bookmark)
            .setView(dialogView)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val title = titleInput.text.toString().trim()
                val url = urlInput.text.toString().trim()
                if (title.isNotEmpty() && url.isNotEmpty()) {
                    lifecycleScope.launch {
                        bookmarkDao.insert(Bookmark(title = title, url = url))
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showBookmarkOptionsDialog(bookmark: Bookmark) {
        val options = arrayOf(getString(R.string.edit_bookmark), getString(R.string.remove_bookmark))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(bookmark.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditBookmarkDialog(bookmark)
                    1 -> showDeleteBookmarkDialog(bookmark)
                }
            }
            .show()
    }

    private fun showEditBookmarkDialog(bookmark: Bookmark) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_bookmark, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.input_title)
        val urlInput = dialogView.findViewById<TextInputEditText>(R.id.input_url)

        titleInput.setText(bookmark.title)
        urlInput.setText(bookmark.url)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_bookmark)
            .setView(dialogView)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val title = titleInput.text.toString().trim()
                val url = urlInput.text.toString().trim()
                if (title.isNotEmpty() && url.isNotEmpty()) {
                    lifecycleScope.launch {
                        bookmarkDao.update(bookmark.copy(title = title, url = url, updatedAt = System.currentTimeMillis()))
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showDeleteBookmarkDialog(bookmark: Bookmark) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.remove_bookmark)
            .setMessage("Remove \"${bookmark.title}\"?")
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                lifecycleScope.launch {
                    bookmarkDao.delete(bookmark)
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
