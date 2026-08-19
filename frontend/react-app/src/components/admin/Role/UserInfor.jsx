import Button from "../../common/Button";
import { useMemo, useState } from "react";
import { Search, UserRound, CheckCircle2, LockKeyhole } from "lucide-react";

export default function UserInfor({
  users = [],
  selectedUser = null,
  mode = "view",
  onChange,
}) {
  const [keyword, setKeyword] = useState("");

  const isView = mode === "view";

  const filteredUsers = useMemo(() => {
    const search = keyword.trim().toLowerCase();

    if (!search) return users;

    return users.filter((user) => {
      return (
        user.email?.toLowerCase().includes(search) ||
        user.phone?.toLowerCase().includes(search)
      );
    });
  }, [users, keyword]);

  const handleSelectUser = (user) => {
    if (isView) return;

    onChange?.(user);
    setKeyword("");
  };

  return (
    <div className="rounded-xl border border-gray-700 bg-gray-900 p-5">

      {/* HEADER */}
      <div className="mb-5 flex items-start gap-3">

        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-blue-500/10 text-blue-400">
          <UserRound size={20} />
        </div>

        <div>
          <h3 className="text-base font-semibold text-white">
            Thông tin người dùng
          </h3>

          <p className="mt-1 text-sm text-gray-400">
            Chọn người dùng được gán role này.
          </p>
        </div>

      </div>

      {/* SEARCH USER */}
      {!isView && (
        <div className="mb-4">

          <label className="mb-2 block text-sm font-medium text-gray-300">
            Người dùng
          </label>

          <div className="relative">

            <Search
              size={18}
              className="
                absolute left-3 top-1/2
                -translate-y-1/2
                text-gray-500
              "
            />

            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Tìm theo email hoặc số điện thoại..."
              className="
                w-full rounded-lg
                border border-gray-700
                bg-gray-800
                py-2.5 pl-10 pr-4
                text-sm text-white
                outline-none
                placeholder:text-gray-500
                focus:border-orange-500
              "
            />

          </div>

        </div>
      )}

      {/* SELECTED USER */}
      {selectedUser ? (
        <div className="
          rounded-lg
          border border-orange-500/30
          bg-orange-500/5
          p-4
        ">

          <div className="flex items-start justify-between gap-4">

            <div className="flex min-w-0 items-start gap-3">

              <div className="
                flex h-10 w-10 shrink-0
                items-center justify-center
                rounded-full
                bg-gray-800
                text-gray-300
              ">
                <UserRound size={18} />
              </div>

              <div className="min-w-0">

                <p className="truncate text-sm font-semibold text-white">
                  {selectedUser.name ||
                    selectedUser.full_name ||
                    "Người dùng"}
                </p>

                <p className="mt-1 truncate text-sm text-gray-400">
                  {selectedUser.email || "Chưa có email"}
                </p>

                {selectedUser.phone && (
                  <p className="mt-1 text-xs text-gray-500">
                    {selectedUser.phone}
                  </p>
                )}

              </div>

            </div>

            {/* STATUS */}
            <div className="shrink-0">

              {selectedUser.locked ? (
                <span className="
                  inline-flex items-center gap-1.5
                  rounded-full
                  bg-red-500/10
                  px-2.5 py-1
                  text-xs font-medium
                  text-red-400
                ">
                  <LockKeyhole size={13} />
                  Locked
                </span>
              ) : selectedUser.active ? (
                <span className="
                  inline-flex items-center gap-1.5
                  rounded-full
                  bg-green-500/10
                  px-2.5 py-1
                  text-xs font-medium
                  text-green-400
                ">
                  <CheckCircle2 size={13} />
                  Active
                </span>
              ) : (
                <span className="
                  inline-flex items-center gap-1.5
                  rounded-full
                  bg-gray-500/10
                  px-2.5 py-1
                  text-xs font-medium
                  text-gray-400
                >
                  Inactive
                </span>
              )}

            </div>

          </div>

        </div>
      ) : (
        <div className="
          rounded-lg
          border border-dashed border-gray-700
          px-4 py-6
          text-center
          text-sm text-gray-500
        ">
          Chưa chọn người dùng.
        </div>
      )}

      {/* USER LIST */}
      {!isView && keyword && (
        <div className="mt-3 overflow-hidden rounded-lg border border-gray-700 bg-gray-800">

          {filteredUsers.length === 0 ? (
            <div className="px-4 py-5 text-center text-sm text-gray-500">
              Không tìm thấy người dùng.
            </div>
          ) : (
            <div className="max-h-56 overflow-y-auto">

              {filteredUsers.map((user) => {

                const isSelected =
                  selectedUser?.id === user.id;

                return (
                  <Button
                    key={user.id}
                    type="button"
                    disabled={isSelected}
                    onClick={() => handleSelectUser(user)}
                    className={`
                      flex w-full items-center gap-3
                      border-b border-gray-700
                      px-4 py-3
                      text-left
                      last:border-b-0
                      transition
                      ${
                        isSelected
                          ? "cursor-default bg-orange-500/5"
                          : "hover:bg-gray-700"
                      }
                    `}
                  >

                    <div className="
                      flex h-9 w-9 shrink-0
                      items-center justify-center
                      rounded-full
                      bg-gray-700
                      text-gray-300
                    ">
                      <UserRound size={16} />
                    </div>

                    <div className="min-w-0 flex-1">

                      <p className="truncate text-sm font-medium text-gray-200">
                        {user.name ||
                          user.full_name ||
                          "Người dùng"}
                      </p>

                      <p className="truncate text-xs text-gray-500">
                        {user.email}
                      </p>

                    </div>

                    {isSelected && (
                      <CheckCircle2
                        size={18}
                        className="text-orange-400"
                      />
                    )}

                  </Button>
                );
              })}

            </div>
          )}

        </div>
      )}

    </div>
  );
}